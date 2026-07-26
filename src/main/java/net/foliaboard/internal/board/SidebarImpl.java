package net.foliaboard.internal.board;

import net.foliaboard.api.Sidebar;
import net.foliaboard.api.format.NumberFormat;
import net.foliaboard.api.hook.LineProcessor;
import net.foliaboard.internal.packet.DisplaySlotType;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SidebarImpl implements Sidebar {
    private record LineData(Component text, NumberFormat format) {
    }

    private final Plugin plugin;
    private final PacketAdapter adapter;
    private final Player player;
    private final String objectiveId;
    private final List<LineProcessor> processors;
    private volatile List<LineProcessor> localProcessors;

    private Component desiredTitle = Component.empty();
    private final List<LineData> desiredLines = new ArrayList<>();
    private boolean desiredVisible = true;

    private boolean created = false;
    private Component sentTitle = Component.empty();
    private final List<LineData> sentLines = new ArrayList<>();

    private static final int VANILLA_MAX_LINES = 15;

    private static final int OVERSIZE_CHARS = 512;

    private static final int OVERSIZE_RECHECK_FLUSHES = 40;

    private static final int MAX_LINE_INDEX = 63;

    private volatile boolean closed = false;
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);
    private boolean warnedLineCount = false;
    private boolean warnedOversize = false;
    private long flushCount = 0;

    public SidebarImpl(Plugin plugin, PacketAdapter adapter, Player player, String objectiveId,
                       List<LineProcessor> processors) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.player = player;
        this.objectiveId = objectiveId;
        this.processors = processors;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    @Override
    public @NotNull Component title() {
        synchronized (this) {
            return desiredTitle;
        }
    }

    @Override
    public @NotNull List<Component> lines() {
        synchronized (this) {
            List<Component> out = new ArrayList<>(desiredLines.size());
            for (LineData line : desiredLines) {
                out.add(line.text());
            }
            return List.copyOf(out);
        }
    }

    @Override
    public int lineCount() {
        synchronized (this) {
            return desiredLines.size();
        }
    }

    @Override
    public @NotNull Sidebar title(@NotNull ComponentLike title) {
        synchronized (this) {
            desiredTitle = title.asComponent();
        }
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar line(int index, @NotNull ComponentLike text) {
        return line(index, text, null);
    }

    @Override
    public @NotNull Sidebar line(int index, @NotNull ComponentLike text, @NotNull NumberFormat numberFormat) {
        return line(index, text, (Object) numberFormat);
    }

    private Sidebar line(int index, ComponentLike text, Object numberFormat) {
        if (index < 0 || index > MAX_LINE_INDEX) {
            throw new IllegalArgumentException("line index " + index + " out of range 0.."
                    + MAX_LINE_INDEX + " (a large index is almost always a typo; the client shows 15 lines)");
        }
        synchronized (this) {
            while (desiredLines.size() <= index) {
                desiredLines.add(new LineData(Component.empty(), null));
            }
            desiredLines.set(index, new LineData(text.asComponent(), (NumberFormat) numberFormat));
        }
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar lines(@NotNull List<? extends ComponentLike> lines) {
        synchronized (this) {
            desiredLines.clear();
            for (ComponentLike l : lines) {
                desiredLines.add(new LineData(l == null ? Component.empty() : l.asComponent(), null));
            }
        }
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar removeLine(int index) {
        synchronized (this) {
            if (index >= 0 && index < desiredLines.size()) {
                desiredLines.remove(index);
            }
        }
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar clearLines() {
        synchronized (this) {
            desiredLines.clear();
        }
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar lineProcessors(@NotNull List<LineProcessor> processors) {
        this.localProcessors = processors.isEmpty() ? null : List.copyOf(processors);
        scheduleFlush();
        return this;
    }

    @Override
    public @NotNull Sidebar visible(boolean visible) {
        synchronized (this) {
            desiredVisible = visible;
        }
        scheduleFlush();
        return this;
    }

    @Override
    public boolean visible() {
        synchronized (this) {
            return desiredVisible;
        }
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Schedulers.onEntity(plugin, player, () -> {
            if (created) {
                adapter.removeObjective(player, objectiveId);
                created = false;
            }
        });
    }

    private void scheduleFlush() {
        if (closed) {
            return;
        }
        if (flushScheduled.compareAndSet(false, true)) {
            boolean scheduled = Schedulers.onEntity(plugin, player, this::flush,
                    () -> flushScheduled.set(false));
            if (!scheduled) {
                flushScheduled.set(false);
            }
        }
    }

    private void flush() {
        flushScheduled.set(false);
        if (closed || !player.isOnline()) {
            return;
        }

        Component title;
        List<LineData> lines;
        boolean show;
        synchronized (this) {
            title = desiredTitle;
            lines = new ArrayList<>(desiredLines);
            show = desiredVisible;
        }

        if (!show) {
            if (created) {
                adapter.removeObjective(player, objectiveId);
                created = false;
                sentLines.clear();
                sentTitle = Component.empty();
            }
            return;
        }

        flushCount++;

        if (lines.size() > VANILLA_MAX_LINES) {
            if (!warnedLineCount) {
                warnedLineCount = true;
                plugin.getLogger().warning("FoliaBoard: sidebar for " + player.getName() + " has "
                        + lines.size() + " lines; the client only shows " + VANILLA_MAX_LINES
                        + " - extra lines are dropped.");
            }
            lines.subList(VANILLA_MAX_LINES, lines.size()).clear();
        }

        title = process(LineProcessor.TITLE, title);
        for (int i = 0; i < lines.size(); i++) {
            LineData line = lines.get(i);
            lines.set(i, new LineData(process(i, line.text()), line.format()));
        }

        if (!warnedOversize && (flushCount == 1 || flushCount % OVERSIZE_RECHECK_FLUSHES == 0)) {
            checkOversize(title, "title");
            for (int i = 0; i < lines.size() && !warnedOversize; i++) {
                checkOversize(lines.get(i).text(), "line " + i);
            }
        }

        if (!created) {
            adapter.createObjective(player, objectiveId, title);
            adapter.setDisplaySlot(player, objectiveId, DisplaySlotType.SIDEBAR);
            created = true;
            sentTitle = title;
            sentLines.clear();
        } else if (!Objects.equals(sentTitle, title)) {
            adapter.updateObjective(player, objectiveId, title);
            sentTitle = title;
        }

        int newCount = lines.size();
        int oldCount = sentLines.size();

        for (int i = 0; i < newCount; i++) {
            LineData line = lines.get(i);
            if (i >= oldCount || !Objects.equals(sentLines.get(i), line)) {
                NumberFormat format = line.format() != null ? line.format() : NumberFormat.blank();
                adapter.setScore(player, objectiveId, entry(i), -i, line.text(), format);
            }
        }

        for (int i = newCount; i < oldCount; i++) {
            adapter.resetScore(player, objectiveId, entry(i));
        }

        sentLines.clear();
        sentLines.addAll(lines);
    }

    private void checkOversize(Component component, String what) {
        if (warnedOversize) {
            return;
        }
        int len = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component).length();
        if (len > OVERSIZE_CHARS) {
            warnedOversize = true;
            plugin.getLogger().warning("FoliaBoard: sidebar " + what + " for " + player.getName()
                    + " is " + len + " chars; unusually large components bloat every packet.");
        }
    }

    private Component process(int index, Component input) {
        Component out = input;
        if (processors != null) {
            for (LineProcessor processor : processors) {
                out = processor.process(player, index, out);
            }
        }
        List<LineProcessor> local = localProcessors;
        if (local != null) {
            for (LineProcessor processor : local) {
                out = processor.process(player, index, out);
            }
        }
        return out;
    }

    private static volatile String[] ENTRY_CACHE = new String[0];

    private String entry(int index) {
        String[] cache = ENTRY_CACHE;
        if (index < cache.length) {
            return cache[index];
        }
        synchronized (SidebarImpl.class) {
            if (index >= ENTRY_CACHE.length) {
                String[] grown = new String[index + 8];
                System.arraycopy(ENTRY_CACHE, 0, grown, 0, ENTRY_CACHE.length);
                for (int i = ENTRY_CACHE.length; i < grown.length; i++) {
                    grown[i] = "L" + i;
                }
                ENTRY_CACHE = grown;
            }
            return ENTRY_CACHE[index];
        }
    }

    String objectiveId() {
        return objectiveId;
    }
}
