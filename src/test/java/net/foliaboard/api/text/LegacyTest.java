package net.foliaboard.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTest {

    @Test
    void convertsAmpersandAndSectionCodes() {
        assertEquals("<reset><green>Hi <bold>there", Legacy.toMini("&aHi &lthere"));
        assertEquals("<reset><red>X", Legacy.toMini("§cX"));
    }

    @Test
    void convertsBothHexForms() {
        assertEquals("<reset><#ff00aa>A", Legacy.toMini("&#ff00aaA"));
        assertEquals("<reset><#123456>B", Legacy.toMini("§x§1§2§3§4§5§6B"));
    }

    @Test
    void leavesPlainAmpersandsAlone() {
        assertEquals("Tom & Jerry &z", Legacy.toMini("Tom & Jerry &z"));
        assertFalse(Legacy.hasCodes("Tom & Jerry"));
        assertTrue(Legacy.hasCodes("&6Gold"));
    }

    @Test
    void stripRemovesEveryCode() {
        assertEquals("Hi there", Legacy.strip("&aHi §lthere&#ffffff"));
    }

    @Test
    void parsedTextKeepsColors() {
        Component parsed = Text.parse("&6Gold <blue>Blue");
        assertEquals("Gold Blue", Text.plain(parsed));
        assertTrue(hasColor(parsed, NamedTextColor.GOLD));
        assertTrue(hasColor(parsed, NamedTextColor.BLUE));
    }

    @Test
    void escapeNeutralisesTags() {
        assertEquals("\\<red>x", Text.escape("<red>x"));
        assertEquals("<red>x", Text.plain(Text.mini(Text.escape("<red>x"))));
    }

    private static boolean hasColor(Component component, TextColor color) {
        if (color.equals(component.color())) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasColor(child, color)) {
                return true;
            }
        }
        return false;
    }
}
