package net.foliaboard.internal;

import net.foliaboard.api.format.NumberFormat;
import net.foliaboard.internal.board.SidebarImpl;
import net.foliaboard.internal.nametag.NametagImpl;
import net.foliaboard.internal.objective.ScoreObjectiveImpl;
import net.foliaboard.internal.packet.DisplaySlotType;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.packet.TeamData;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

/**
 * Tests the packet <i>decisions</i> the managers make - diffing, idempotent init, create-vs-modify -
 * against a recording adapter, with the scheduler run inline. This is the layer that produced the
 * duplicate-objective crash; it's now covered.
 */
class ManagerLogicTest {

    /** Records how many of each packet the managers ask for. */
    static final class Recording implements PacketAdapter {
        int createObjective, updateObjective, removeObjective, setDisplaySlot, setScore, resetScore;
        int createTeam, updateTeam, removeTeam, teamEntries;

        public String describe() {
            return "recording";
        }

        public void createObjective(Player v, String id, Component t) {
            createObjective++;
        }

        public void updateObjective(Player v, String id, Component t) {
            updateObjective++;
        }

        public void removeObjective(Player v, String id) {
            removeObjective++;
        }

        public void setDisplaySlot(Player v, String id, DisplaySlotType s) {
            setDisplaySlot++;
        }

        public void setScore(Player v, String id, String e, int val, Component d, NumberFormat f) {
            setScore++;
        }

        public void resetScore(Player v, String id, String e) {
            resetScore++;
        }

        public void createTeam(Player v, TeamData t, Collection<String> e) {
            createTeam++;
        }

        public void updateTeam(Player v, TeamData t) {
            updateTeam++;
        }

        public void removeTeam(Player v, String name) {
            removeTeam++;
        }

        public void teamEntries(Player v, String name, Collection<String> e, boolean add) {
            teamEntries++;
        }
    }

    private Plugin plugin;
    private Player player;
    private Recording adapter;

    @BeforeEach
    void setUp() {
        Schedulers.setSynchronousForTesting(true);
        plugin = Mockito.mock(Plugin.class);
        lenient().when(plugin.isEnabled()).thenReturn(true);
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        player = Mockito.mock(Player.class);
        lenient().when(player.isOnline()).thenReturn(true);
        lenient().when(player.getName()).thenReturn("Steve");
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        adapter = new Recording();
    }

    @AfterEach
    void tearDown() {
        Schedulers.setSynchronousForTesting(false);
    }

    @Test
    void sidebarSendsOnlyChangedLines() {
        SidebarImpl sb = new SidebarImpl(plugin, adapter, player, "obj", List.of());
        sb.title(Component.text("Title"));
        sb.line(0, Component.text("A"));
        sb.line(1, Component.text("B"));

        assertEquals(1, adapter.createObjective, "objective created once");
        assertEquals(1, adapter.setDisplaySlot);
        assertEquals(2, adapter.setScore, "one score per line");

        int before = adapter.setScore;
        sb.line(0, Component.text("A2"));           // change one line
        assertEquals(before + 1, adapter.setScore, "only the changed line is resent");

        int after = adapter.setScore;
        sb.line(0, Component.text("A2"));           // set the same value again
        assertEquals(after, adapter.setScore, "unchanged line produces no packet");
    }

    @Test
    void sidebarNeverSendsMoreThanTheClientLimit() {
        SidebarImpl sb = new SidebarImpl(plugin, adapter, player, "obj", List.of());
        for (int i = 0; i < 25; i++) {
            sb.line(i, Component.text("L" + i));   // 25 lines, but the client shows 15
        }
        assertEquals(15, adapter.setScore, "lines beyond the 15-line cap must not be sent");
    }

    @Test
    void absurdLineIndexIsRejected() {
        SidebarImpl sb = new SidebarImpl(plugin, adapter, player, "obj", List.of());
        assertThrows(IllegalArgumentException.class, () -> sb.line(1000, Component.text("typo")));
    }

    @Test
    void belowNameInitIsIdempotent() {
        ScoreObjectiveImpl obj = new ScoreObjectiveImpl(plugin, adapter, "fb_bn", DisplaySlotType.BELOW_NAME);
        obj.onJoin(player);
        obj.onJoin(player);   // second init must NOT re-create (this was the duplicate-objective crash)
        assertEquals(1, adapter.createObjective);
    }

    @Test
    void nametagCreatesThenModifies() {
        Player target = Mockito.mock(Player.class);
        lenient().when(target.getName()).thenReturn("Alex");
        NametagImpl tag = new NametagImpl(plugin, adapter, target, "team1", List::of);

        tag.applyTo(player);
        tag.applyTo(player);   // second apply to same viewer must modify, not re-create

        assertEquals(1, adapter.createTeam);
        assertEquals(1, adapter.updateTeam);
    }
}
