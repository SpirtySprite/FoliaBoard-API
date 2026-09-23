package net.foliaboard.api;

import net.foliaboard.FoliaBoard;
import net.foliaboard.internal.bossbar.ManagedBossBarImpl;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class BossBarBuilder {
    private final FoliaBoard board;
    private final Player player;
    private final String id;
    private Function<Player, String> text = viewer -> "";
    private ToDoubleFunction<Player> progress = viewer -> 1.0D;
    private Function<Player, BossBar.Color> color = viewer -> BossBar.Color.PURPLE;
    private BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
    private boolean placeholders;
    private int refreshTicks = -1;
    private long lifetimeTicks = -1L;

    public BossBarBuilder(@NotNull FoliaBoard board, @NotNull Player player, @NotNull String id) {
        this.board = board;
        this.player = player;
        this.id = id;
    }

    public @NotNull BossBarBuilder text(@NotNull String anyFormat) {
        this.text = viewer -> anyFormat;
        return this;
    }

    public @NotNull BossBarBuilder text(@NotNull Function<Player, String> text) {
        this.text = text;
        return this;
    }

    public @NotNull BossBarBuilder progress(double progress) {
        double clamped = clamp(progress);
        this.progress = viewer -> clamped;
        return this;
    }

    public @NotNull BossBarBuilder progress(@NotNull ToDoubleFunction<Player> progress) {
        this.progress = progress;
        return this;
    }

    public @NotNull BossBarBuilder color(@NotNull BossBar.Color color) {
        this.color = viewer -> color;
        return this;
    }

    public @NotNull BossBarBuilder color(@NotNull Function<Player, BossBar.Color> color) {
        this.color = color;
        return this;
    }

    public @NotNull BossBarBuilder overlay(@NotNull BossBar.Overlay overlay) {
        this.overlay = overlay;
        return this;
    }

    public @NotNull BossBarBuilder placeholders(boolean enabled) {
        this.placeholders = enabled;
        return this;
    }

    public @NotNull BossBarBuilder refreshEvery(int ticks) {
        this.refreshTicks = Math.max(1, ticks);
        return this;
    }

    public @NotNull BossBarBuilder hideAfter(long ticks) {
        this.lifetimeTicks = Math.max(1L, ticks);
        return this;
    }

    public @NotNull ManagedBossBar show() {
        ManagedBossBarImpl bar = new ManagedBossBarImpl(board.plugin(), board.placeholders(), player, id,
                new ManagedBossBarImpl.Spec(text, progress, color, overlay, placeholders, refreshTicks, lifetimeTicks),
                board::forgetBossBar);
        board.trackBossBar(player, id, bar);
        bar.start();
        return bar;
    }

    static double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
