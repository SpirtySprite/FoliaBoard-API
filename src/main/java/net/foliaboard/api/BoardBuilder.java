package net.foliaboard.api;

import net.foliaboard.FoliaBoard;
import net.foliaboard.api.animation.Animation;
import net.foliaboard.api.format.NumberFormat;
import net.foliaboard.api.text.Text;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;
import java.util.function.Function;

public final class BoardBuilder {
    private record LineSpec(Function<Player, Component> renderer, NumberFormat format, boolean dynamic) {
    }

    private final FoliaBoard board;
    private final Player player;

    private Function<Player, Component> titleRenderer = p -> Component.empty();
    private boolean titleDynamic = false;
    private final TreeMap<Integer, LineSpec> lines = new TreeMap<>();
    private final java.util.List<net.foliaboard.api.hook.LineProcessor> processors = new java.util.ArrayList<>();
    private int nextAutoIndex = 0;
    private boolean parsePlaceholders = false;
    private int refreshTicks = -1;

    public BoardBuilder(@NotNull FoliaBoard board, @NotNull Player player) {
        this.board = board;
        this.player = player;
    }

    public @NotNull BoardBuilder placeholders(boolean enabled) {
        this.parsePlaceholders = enabled;
        return this;
    }

    public @NotNull BoardBuilder refreshEvery(int ticks) {
        this.refreshTicks = Math.max(1, ticks);
        return this;
    }

    public @NotNull BoardBuilder processor(@NotNull net.foliaboard.api.hook.LineProcessor processor) {
        this.processors.add(processor);
        return this;
    }

    public @NotNull BoardBuilder title(@NotNull String miniMessage) {
        Rendered r = render(miniMessage);
        this.titleRenderer = r.renderer;
        this.titleDynamic = r.dynamic;
        return this;
    }

    public @NotNull BoardBuilder title(@NotNull ComponentLike title) {
        Component c = title.asComponent();
        this.titleRenderer = p -> c;
        this.titleDynamic = false;
        return this;
    }

    public @NotNull BoardBuilder title(@NotNull Animation<Component> animation) {
        this.titleRenderer = p -> animation.current();
        this.titleDynamic = true;
        return this;
    }

    public @NotNull BoardBuilder line(int index, @NotNull String miniMessage) {
        Rendered r = render(miniMessage);
        lines.put(index, new LineSpec(r.renderer, null, r.dynamic));
        return this;
    }

    public @NotNull BoardBuilder line(int index, @NotNull String miniMessage, @NotNull NumberFormat format) {
        Rendered r = render(miniMessage);
        lines.put(index, new LineSpec(r.renderer, format, r.dynamic));
        return this;
    }

    public @NotNull BoardBuilder line(int index, @NotNull ComponentLike component) {
        Component c = component.asComponent();
        lines.put(index, new LineSpec(p -> c, null, false));
        return this;
    }

    public @NotNull BoardBuilder line(int index, @NotNull Animation<Component> animation) {
        lines.put(index, new LineSpec(p -> animation.current(), null, true));
        return this;
    }

    public @NotNull BoardBuilder line(@NotNull String miniMessage) {
        return line(nextAutoIndex++, miniMessage);
    }

    public @NotNull BoardBuilder line(@NotNull String miniMessage, @NotNull NumberFormat format) {
        return line(nextAutoIndex++, miniMessage, format);
    }

    public @NotNull BoardBuilder line(@NotNull ComponentLike component) {
        return line(nextAutoIndex++, component);
    }

    public @NotNull BoardBuilder line(@NotNull Animation<Component> animation) {
        return line(nextAutoIndex++, animation);
    }

    public @NotNull BoardBuilder blankLine() {
        return line(nextAutoIndex++, Component.empty());
    }

    public @NotNull BoardBuilder lines(@NotNull String... miniMessageLines) {
        for (String line : miniMessageLines) {
            line(nextAutoIndex++, line);
        }
        return this;
    }

    public @NotNull BoardBuilder lines(@NotNull Iterable<String> miniMessageLines) {
        for (String line : miniMessageLines) {
            line(nextAutoIndex++, line);
        }
        return this;
    }

    public @NotNull Sidebar build() {
        board.markBuilderOwned(player);
        Sidebar sidebar = board.sidebar(player);
        sidebar.lineProcessors(processors);
        int maxIndex = lines.isEmpty() ? -1 : lines.lastKey();
        boolean dynamic = titleDynamic || lines.values().stream().anyMatch(LineSpec::dynamic);

        Runnable apply = () -> {
            if (sidebar.closed() || !player.isOnline()) {
                return;
            }

            sidebar.clearLines();
            sidebar.title(titleRenderer.apply(player));
            for (int i = 0; i <= maxIndex; i++) {
                LineSpec spec = lines.get(i);
                if (spec == null) {
                    sidebar.line(i, Component.empty());
                } else if (spec.format() != null) {
                    sidebar.line(i, spec.renderer().apply(player), spec.format());
                } else {
                    sidebar.line(i, spec.renderer().apply(player));
                }
            }
        };

        Schedulers.onEntity(board.plugin(), player, apply);

        if (dynamic) {
            int interval = refreshTicks > 0 ? refreshTicks : 3;
            Schedulers.ScheduledHandle handle = Schedulers.entityTimer(board.plugin(), player, h -> {
                if (sidebar.closed() || !player.isOnline()) {
                    h.cancel();
                    return;
                }
                board.recordRefresh();
                apply.run();
            }, interval, interval);

            board.trackRefresh(player, handle);
        } else {
            board.trackRefresh(player, null);
        }
        return sidebar;
    }

    private record Rendered(Function<Player, Component> renderer, boolean dynamic) {
    }

    private Rendered render(String raw) {
        if (parsePlaceholders && raw.indexOf('%') >= 0) {
            return new Rendered(new CachingRenderer(raw), true);
        }
        Component parsed = Text.mini(raw);
        return new Rendered(p -> parsed, false);
    }

    private final class CachingRenderer implements Function<Player, Component> {
        private final String raw;
        private String lastResolved;
        private Component lastComponent;

        CachingRenderer(String raw) {
            this.raw = raw;
        }

        @Override
        public Component apply(Player player) {
            String resolved = board.placeholders().resolveForMiniMessage(player, raw);
            if (!resolved.equals(lastResolved)) {
                lastResolved = resolved;
                lastComponent = Text.mini(resolved);
            }
            return lastComponent;
        }
    }
}
