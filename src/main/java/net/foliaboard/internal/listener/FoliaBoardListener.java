package net.foliaboard.internal.listener;

import net.foliaboard.FoliaBoard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FoliaBoardListener implements Listener {
    private final FoliaBoard foliaBoard;

    public FoliaBoardListener(FoliaBoard foliaBoard) {
        this.foliaBoard = foliaBoard;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        foliaBoard.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        foliaBoard.handleWorldChange(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        foliaBoard.handleQuit(event.getPlayer());
    }
}
