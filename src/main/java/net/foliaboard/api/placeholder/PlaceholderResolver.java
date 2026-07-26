package net.foliaboard.api.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PlaceholderResolver {
    @Nullable String resolve(@NotNull Player player, @NotNull String key);
}
