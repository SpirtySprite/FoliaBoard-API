package net.foliaboard.internal.bossbar;

import net.foliaboard.api.ManagedBossBar;
import net.foliaboard.api.placeholder.Placeholders;
import net.foliaboard.api.text.Text;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class ManagedBossBarImpl implements ManagedBossBar {
    private static final int DEFAULT_PLACEHOLDER_REFRESH = 20;

    public record Spec(Function<Player, String> text, ToDoubleFunction<Player> progress,
                       Function<Player, BossBar.Color> color, BossBar.Overlay overlay,
                       boolean placeholders, int refreshTicks, long lifetimeTicks) {
    }

    private final Plugin plugin;
    private final Placeholders placeholders;
    private final Player player;
    private final String id;
    private final Spec spec;
    private final BiConsumer<Player, String> onHide;
    private final BossBar bar;
    private volatile boolean hidden;
    private volatile Schedulers.ScheduledHandle timer;
    private String sentText;
    private long ticksAlive;

    public ManagedBossBarImpl(Plugin plugin, Placeholders placeholders, Player player, String id, Spec spec,
                              BiConsumer<Player, String> onHide) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.player = player;
        this.id = id;
        this.spec = spec;
        this.onHide = onHide;
        this.bar = BossBar.bossBar(Component.empty(), 1.0F, BossBar.Color.PURPLE, spec.overlay());
    }

    public void start() {
        Schedulers.onEntity(plugin, player, () -> {
            update();
            if (!hidden) {
                player.showBossBar(bar);
            }
        });
        int interval = spec.refreshTicks() > 0 ? spec.refreshTicks()
                : spec.placeholders() ? DEFAULT_PLACEHOLDER_REFRESH : -1;
        if (interval <= 0 && spec.lifetimeTicks() > 0) {
            interval = (int) Math.min(Integer.MAX_VALUE, spec.lifetimeTicks());
        }
        if (interval > 0) {
            int period = interval;
            timer = Schedulers.entityTimer(plugin, player, handle -> {
                if (hidden || !player.isOnline()) {
                    handle.cancel();
                    return;
                }
                ticksAlive += period;
                if (spec.lifetimeTicks() > 0 && ticksAlive >= spec.lifetimeTicks()) {
                    hide();
                    return;
                }
                update();
            }, interval, interval);
        }
    }

    public synchronized void update() {
        if (hidden) {
            return;
        }
        String raw = spec.text().apply(player);
        String text = raw == null ? "" : spec.placeholders() ? placeholders.resolveForMiniMessage(player, raw) : raw;
        if (!text.equals(sentText)) {
            bar.name(Text.cached(text));
            sentText = text;
        }
        float progress = (float) clamp(spec.progress().applyAsDouble(player));
        if (bar.progress() != progress) {
            bar.progress(progress);
        }
        BossBar.Color color = spec.color().apply(player);
        if (color != null && bar.color() != color) {
            bar.color(color);
        }
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    @Override
    public @NotNull BossBar bar() {
        return bar;
    }

    @Override
    public @NotNull ManagedBossBar refresh() {
        Schedulers.onEntity(plugin, player, this::update);
        return this;
    }

    @Override
    public void hide() {
        if (hidden) {
            return;
        }
        hidden = true;
        Schedulers.ScheduledHandle running = timer;
        timer = null;
        if (running != null) {
            running.cancel();
        }
        Schedulers.onEntity(plugin, player, () -> player.hideBossBar(bar));
        onHide.accept(player, id);
    }

    public void hideSilently() {
        if (hidden) {
            return;
        }
        hidden = true;
        Schedulers.ScheduledHandle running = timer;
        timer = null;
        if (running != null) {
            running.cancel();
        }
        Schedulers.onEntity(plugin, player, () -> player.hideBossBar(bar));
    }

    @Override
    public boolean hidden() {
        return hidden;
    }
}
