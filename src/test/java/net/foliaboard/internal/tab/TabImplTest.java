package net.foliaboard.internal.tab;

import net.foliaboard.api.placeholder.Placeholders;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TabImplTest {

    private Plugin plugin;
    private Player player;

    @BeforeEach
    void setUp() {
        Schedulers.setSynchronousForTesting(true);
        plugin = Mockito.mock(Plugin.class);
        player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(player.isOnline()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Schedulers.setSynchronousForTesting(false);
    }

    @Test
    void unchangedContentIsNotResent() {
        TabImpl tab = new TabImpl(plugin, new Placeholders(), player, new TabImpl.Spec(
                p -> "<gold>Header", p -> "Footer", p -> "&aSteve", null, false, -1, true));
        tab.update();
        tab.update();
        tab.update();
        verify(player, times(1)).sendPlayerListHeaderAndFooter(any(Component.class), any(Component.class));
        verify(player, times(1)).playerListName(any(Component.class));
        assertEquals(2, tab.sentUpdates());
    }

    @Test
    void onlyTheChangedPartIsSent() {
        AtomicReference<String> footer = new AtomicReference<>("A");
        TabImpl tab = new TabImpl(plugin, new Placeholders(), player, new TabImpl.Spec(
                p -> "Header", p -> footer.get(), p -> "Steve", null, false, -1, true));
        tab.update();
        footer.set("B");
        tab.update();
        verify(player, times(2)).sendPlayerListHeaderAndFooter(any(Component.class), any(Component.class));
        verify(player, times(1)).playerListName(any(Component.class));
    }

    @Test
    void placeholdersAreResolvedPerRefresh() {
        Placeholders placeholders = new Placeholders();
        AtomicReference<String> rank = new AtomicReference<>("VIP");
        placeholders.register("rank", p -> rank.get());
        TabImpl tab = new TabImpl(plugin, placeholders, player, new TabImpl.Spec(
                null, null, p -> "%rank% %player%", null, true, -1, true));
        tab.update();
        tab.update();
        rank.set("Admin");
        tab.update();
        verify(player, times(2)).playerListName(any(Component.class));
    }

    @Test
    void closingResetsWhatWasSent() {
        TabImpl tab = new TabImpl(plugin, new Placeholders(), player, new TabImpl.Spec(
                p -> "Header", p -> "Footer", null, null, false, -1, true));
        tab.update();
        tab.close();
        tab.update();
        assertTrue(tab.closed());
        verify(player, times(2)).sendPlayerListHeaderAndFooter(any(Component.class), any(Component.class));
        verify(player, never()).playerListName(any());
    }
}
