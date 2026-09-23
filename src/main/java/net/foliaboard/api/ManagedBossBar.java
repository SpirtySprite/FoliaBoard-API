package net.foliaboard.api;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ManagedBossBar {
    @NotNull String id();

    @NotNull Player player();

    @NotNull BossBar bar();

    @NotNull ManagedBossBar refresh();

    void hide();

    boolean hidden();
}
