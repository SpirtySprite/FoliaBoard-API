package net.foliaboard.internal;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdsTest {

    @Test
    void namespaceIsStableAndCaseInsensitive() {
        assertEquals(Ids.namespace("MyPlugin"), Ids.namespace("myplugin"));
        assertEquals(4, Ids.namespace("MyPlugin").length());
    }

    @Test
    void differentPluginsGetDifferentNamesForTheSameCounter() {
        String a = Ids.namespace("ShopPlugin");
        String b = Ids.namespace("LobbyPlugin");
        assertNotEquals(a, b);
        assertNotEquals(Ids.sidebarObjective(a, 0), Ids.sidebarObjective(b, 0));
        assertNotEquals(Ids.belowNameObjective(a), Ids.belowNameObjective(b));
        assertNotEquals(Ids.tabListObjective(a), Ids.tabListObjective(b));
        assertNotEquals(Ids.team(a, null, 0), Ids.team(b, null, 0));
        assertNotEquals(Ids.team(a, 5, 0), Ids.team(b, 5, 0));
    }

    @Test
    void namesFitTheLengthLimitEvenForLargeCounters() {
        String ns = Ids.namespace("Anything");
        int big = Integer.MAX_VALUE;
        assertTrue(Ids.sidebarObjective(ns, big).length() <= Ids.MAX_LENGTH);
        assertTrue(Ids.team(ns, 9999, big).length() <= Ids.MAX_LENGTH);
        assertTrue(Ids.team(ns, null, big).length() <= Ids.MAX_LENGTH);
    }

    @Test
    void sortWeightedTeamsKeepTheirWeightPrefixSoTheySortCorrectly() {
        String ns = Ids.namespace("Anything");
        assertTrue(Ids.team(ns, 7, 1).startsWith("0007"));
        assertTrue(Ids.team(ns, -3, 1).startsWith("0000"));
        assertTrue(Ids.team(ns, 123456, 1).startsWith("9999"));
    }

    @Test
    void counterProducesUniqueNames() {
        String ns = Ids.namespace("Anything");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(seen.add(Ids.sidebarObjective(ns, i)));
            assertTrue(seen.add(Ids.team(ns, null, i)));
        }
    }
}
