package net.foliaboard.internal.tab;

import net.foliaboard.api.TabList;
import net.foliaboard.api.placeholder.Placeholders;
import net.foliaboard.api.text.Text;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class TabImpl implements TabList {
    private static final int DEFAULT_PLACEHOLDER_REFRESH = 20;

    public record Spec(Function<Player, String> header, Function<Player, String> footer,
                       Function<Player, String> name, ToIntFunction<Player> order,
                       boolean placeholders, int refreshTicks, boolean resetOnClose) {
    }

    private final Plugin plugin;
    private final Placeholders placeholders;
    private final Player player;
    private final Spec spec;
    private volatile boolean closed;
    private volatile Schedulers.ScheduledHandle timer;
    private String sentHeader;
    private String sentFooter;
    private String sentName;
    private Integer sentOrder;
    private int updates;

    public TabImpl(Plugin plugin, Placeholders placeholders, Player player, Spec spec) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.player = player;
        this.spec = spec;
    }

    public void start() {
        Schedulers.onEntity(plugin, player, this::update);
        int interval = spec.refreshTicks() > 0 ? spec.refreshTicks()
                : spec.placeholders() ? DEFAULT_PLACEHOLDER_REFRESH : -1;
        if (interval > 0) {
            timer = Schedulers.entityTimer(plugin, player, handle -> {
                if (closed || !player.isOnline()) {
                    handle.cancel();
                    return;
                }
                update();
            }, interval, interval);
        }
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    @Override
    public @NotNull TabList refresh() {
        Schedulers.onEntity(plugin, player, this::update);
        return this;
    }

    @Override
    public synchronized int sentUpdates() {
        return updates;
    }

    public synchronized void update() {
        if (closed) {
            return;
        }
        String header = render(spec.header());
        String footer = render(spec.footer());
        if ((header != null || footer != null)
                && (!Objects.equals(header, sentHeader) || !Objects.equals(footer, sentFooter))) {
            player.sendPlayerListHeaderAndFooter(component(header), component(footer));
            sentHeader = header;
            sentFooter = footer;
            updates++;
        }
        String name = render(spec.name());
        if (name != null && !name.equals(sentName)) {
            player.playerListName(component(name));
            sentName = name;
            updates++;
        }
        if (spec.order() != null) {
            int order = spec.order().applyAsInt(player);
            if (sentOrder == null || sentOrder != order) {
                TabOrder.set(player, order);
                sentOrder = order;
                updates++;
            }
        }
    }

    private String render(Function<Player, String> source) {
        if (source == null) {
            return null;
        }
        String raw = source.apply(player);
        if (raw == null) {
            return null;
        }
        return spec.placeholders() ? placeholders.resolveForMiniMessage(player, raw) : raw;
    }

    private static Component component(String text) {
        return text == null ? Component.empty() : Text.cached(text);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Schedulers.ScheduledHandle running = timer;
        timer = null;
        if (running != null) {
            running.cancel();
        }
        if (spec.resetOnClose() && player.isOnline()) {
            Schedulers.onEntity(plugin, player, () -> {
                if (sentHeader != null || sentFooter != null) {
                    player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
                }
                if (sentName != null) {
                    player.playerListName(null);
                }
            });
        }
    }

    @Override
    public boolean closed() {
        return closed;
    }
}
