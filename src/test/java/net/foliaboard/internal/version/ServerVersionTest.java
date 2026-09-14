package net.foliaboard.internal.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerVersionTest {

    @Test
    void parsesPlainAndSnapshotStrings() {
        ServerVersion plain = ServerVersion.parse("1.21.4");
        assertEquals(21, plain.minor());
        assertEquals(4, plain.patch());

        ServerVersion snapshot = ServerVersion.parse("1.21.11-R0.1-SNAPSHOT");
        assertEquals(21, snapshot.minor());
        assertEquals(11, snapshot.patch());
    }

    @Test
    void missingPatchDefaultsToZero() {
        ServerVersion v = ServerVersion.parse("1.21");
        assertEquals(21, v.minor());
        assertEquals(0, v.patch());
    }

    @Test
    void minimumSupportedVersionGate() {
        assertTrue(ServerVersion.parse("1.20.6").isAtLeast(20, 6));
        assertTrue(ServerVersion.parse("1.21.11").isAtLeast(20, 6));
        assertFalse(ServerVersion.parse("1.20.1").isAtLeast(20, 6));
        assertFalse(ServerVersion.parse("1.19.4").isAtLeast(20, 6));
    }

    @Test
    void newYearBasedSchemeIsNewerThanAny1x() {
        assertTrue(ServerVersion.parse("26.2.0").isAtLeast(20, 6));
        assertTrue(ServerVersion.parse("26.2").isAtLeast(20, 6));
        assertTrue(ServerVersion.parse("26.2.0").supportsScoreDisplayName());
    }

    @Test
    void scoreDisplayNameRequires1203() {
        assertTrue(ServerVersion.parse("1.20.3").supportsScoreDisplayName());
        assertTrue(ServerVersion.parse("1.21.0").supportsScoreDisplayName());
        assertFalse(ServerVersion.parse("1.20.2").supportsScoreDisplayName());
    }

    @Test
    void patchComparisonIsNumericNotLexical() {
        assertTrue(ServerVersion.parse("1.21.11").isAtLeast(21, 2));
        assertFalse(ServerVersion.parse("1.21.2").isAtLeast(21, 11));
    }
}
