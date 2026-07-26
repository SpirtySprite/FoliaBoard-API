package net.foliaboard.api.event;

import net.foliaboard.api.Sidebar;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class SidebarCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Sidebar sidebar;

    public SidebarCreateEvent(@NotNull Player player, @NotNull Sidebar sidebar) {
        super(false);
        this.player = player;
        this.sidebar = sidebar;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Sidebar getSidebar() {
        return sidebar;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
