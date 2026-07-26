package net.foliaboard.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NametagResolver {
    void resolve(@NotNull Player viewer, @NotNull Player target, @NotNull NametagStyle style);
}
