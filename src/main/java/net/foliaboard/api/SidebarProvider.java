package net.foliaboard.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public interface SidebarProvider {
    @NotNull Component title(@NotNull Player player);

    @NotNull List<Component> lines(@NotNull Player player);

    default boolean visible(@NotNull Player player) {
        return true;
    }

    default int refreshIntervalTicks() {
        return 20;
    }

    static @NotNull SidebarProvider of(@NotNull Function<Player, Component> title,
                                       @NotNull Function<Player, List<Component>> lines) {
        return of(title, lines, 20);
    }

    static @NotNull SidebarProvider of(@NotNull Function<Player, Component> title,
                                       @NotNull Function<Player, List<Component>> lines,
                                       int refreshIntervalTicks) {
        return new SidebarProvider() {
            @Override
            public @NotNull Component title(@NotNull Player player) {
                return title.apply(player);
            }

            @Override
            public @NotNull List<Component> lines(@NotNull Player player) {
                return lines.apply(player);
            }

            @Override
            public int refreshIntervalTicks() {
                return refreshIntervalTicks;
            }
        };
    }
}
