package net.foliaboard.api.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the placeholder engine, including the injection-safety fix: values must not be able to smuggle
 * MiniMessage tags into a board.
 */
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
    void leavesUnknownTokensUntouched() {
        Placeholders ph = new Placeholders();
        assertEquals("x %unknown% y", ph.apply(player(), "x %unknown% y"));
    }

    @Test
    void componentValuesAreEscapedAgainstMiniMessageInjection() {
        Placeholders ph = new Placeholders();
        // A hostile value: if it were parsed as MiniMessage it would inject a click event + red colour.
        ph.register("evil", p -> "<red><click:run_command:/op Steve>hi</click>");
        Component out = ph.component(player(), "Name: %evil%");

        // The tags must survive as literal text, not become styling/click behaviour.
        String plain = PlainTextComponentSerializer.plainText().serialize(out);
        assertTrue(plain.contains("<red>"), "MiniMessage tags in a value must render literally: " + plain);
        assertTrue(plain.contains("<click:run_command:/op Steve>"), plain);
        // And the component must not actually carry a click event.
        assertFalse(hasClick(out), "placeholder value must not inject a click event");
    }

    @Test
    void templateTagsStillWork() {
        Placeholders ph = new Placeholders();
        ph.register("name", p -> "Steve");
        Component out = ph.component(player(), "<green>%name%");
        assertEquals("Steve", PlainTextComponentSerializer.plainText().serialize(out));
        // The <green> from the template (not a value) should still apply.
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
