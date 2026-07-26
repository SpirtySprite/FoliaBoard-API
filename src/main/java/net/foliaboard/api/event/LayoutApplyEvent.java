package net.foliaboard.api.event;

import net.foliaboard.api.layout.Layout;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class LayoutApplyEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private Layout layout;
    private boolean cancelled;

    public LayoutApplyEvent(@NotNull Player player, @NotNull Layout layout) {
        super(false);
        this.player = player;
        this.layout = layout;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Layout getLayout() {
        return layout;
    }

    public void setLayout(@NotNull Layout layout) {
        this.layout = layout;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
