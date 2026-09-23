package net.foliaboard.internal.bossbar;

import net.foliaboard.api.placeholder.Placeholders;
import net.foliaboard.api.text.Text;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class ManagedBossBarImplTest {

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
    void progressIsClampedAndTextFollowsTheSource() {
        AtomicReference<Double> progress = new AtomicReference<>(2.5D);
        ManagedBossBarImpl bar = new ManagedBossBarImpl(plugin, new Placeholders(), player, "event",
                new ManagedBossBarImpl.Spec(p -> "&6XP x2", p -> progress.get(), p -> BossBar.Color.YELLOW,
                        BossBar.Overlay.PROGRESS, false, -1, -1L), (p, id) -> { });
        bar.update();
        assertEquals(1.0F, bar.bar().progress());
        assertEquals("XP x2", Text.plain(bar.bar().name()));
        assertEquals(BossBar.Color.YELLOW, bar.bar().color());
        progress.set(Double.NaN);
        bar.update();
        assertEquals(0.0F, bar.bar().progress());
    }

    @Test
    void hidingNotifiesTheOwnerOnce() {
        List<String> hidden = new ArrayList<>();
        ManagedBossBarImpl bar = new ManagedBossBarImpl(plugin, new Placeholders(), player, "event",
                new ManagedBossBarImpl.Spec(p -> "x", p -> 1.0D, p -> BossBar.Color.RED,
                        BossBar.Overlay.PROGRESS, false, -1, -1L), (p, id) -> hidden.add(id));
        bar.start();
        bar.hide();
        bar.hide();
        assertTrue(bar.hidden());
        assertEquals(List.of("event"), hidden);
        verify(player).showBossBar(bar.bar());
        verify(player).hideBossBar(bar.bar());
    }
}
