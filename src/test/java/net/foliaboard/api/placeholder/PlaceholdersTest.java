package net.foliaboard.api.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholdersTest {

    private Player player() {
        Player p = Mockito.mock(Player.class);
        Mockito.when(p.getName()).thenReturn("Steve");
        return p;
    }

    @Test
    void replacesRegisteredTokens() {
        Placeholders ph = new Placeholders();
        ph.register("rank", p -> "Admin");
        assertEquals("Rank: Admin", ph.apply(player(), "Rank: %rank%"));
    }

    @Test
    void keyedPlaceholdersIgnoreCaseAndCanBeReplaced() {
        Placeholders ph = new Placeholders();
        ph.register("Balance", p -> "10");
        assertEquals("10 10", ph.apply(player(), "%balance% %BALANCE%"));
        ph.register("balance", p -> "20");
        assertEquals("20", ph.apply(player(), "%balance%"));
        ph.unregister("BALANCE");
        assertEquals("%balance%", ph.apply(player(), "%balance%"));
    }

    @Test
    void plainValuesSkipEscapingButBackslashesStayLiteral() {
        Placeholders ph = new Placeholders();
        ph.register("plain", p -> "Steve");
        ph.register("slash", p -> "a\\");
        assertEquals("<green>Steve", ph.resolveForMiniMessage(player(), "<green>%plain%"));
        Component out = ph.component(player(), "%slash%<green>x");
        assertEquals("a\\x", PlainTextComponentSerializer.plainText().serialize(out));
    }

    @Test
    void leavesUnknownTokensUntouched() {
        Placeholders ph = new Placeholders();
        assertEquals("x %unknown% y", ph.apply(player(), "x %unknown% y"));
    }

    @Test
    void componentValuesAreEscapedAgainstMiniMessageInjection() {
        Placeholders ph = new Placeholders();
        ph.register("evil", p -> "<red><click:run_command:/op Steve>hi</click>");
        Component out = ph.component(player(), "Name: %evil%");

        String plain = PlainTextComponentSerializer.plainText().serialize(out);
        assertTrue(plain.contains("<red>"), "MiniMessage tags in a value must render literally: " + plain);
        assertTrue(plain.contains("<click:run_command:/op Steve>"), plain);
        assertFalse(hasClick(out), "placeholder value must not inject a click event");
    }

    @Test
    void templateTagsStillWork() {
        Placeholders ph = new Placeholders();
        ph.register("name", p -> "Steve");
        Component out = ph.component(player(), "<green>%name%");
        assertEquals("Steve", PlainTextComponentSerializer.plainText().serialize(out));
        assertEquals(net.kyori.adventure.text.format.NamedTextColor.GREEN, out.color());
    }

    private static boolean hasClick(Component component) {
        if (component.clickEvent() != null) {
            return true;
        }
        for (Component child : component.children()) {
            if (hasClick(child)) {
                return true;
            }
        }
        return false;
    }
}
