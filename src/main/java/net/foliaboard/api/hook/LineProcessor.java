package net.foliaboard.api.hook;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface LineProcessor {
    int TITLE = -1;

    @NotNull Component process(@NotNull Player player, int index, @NotNull Component line);
}
