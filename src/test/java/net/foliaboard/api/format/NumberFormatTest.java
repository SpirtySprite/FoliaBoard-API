package net.foliaboard.api.format;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The number-format factories are the public surface consumers use; these lock in their shapes so a
 * refactor of the sealed hierarchy can't silently change behaviour.
 */
class NumberFormatTest {

    @Test
    void blankAndDefaultAreSingletons() {
        assertSame(NumberFormat.blank(), NumberFormat.blank());
        assertSame(NumberFormat.defaultFormat(), NumberFormat.defaultFormat());
        assertInstanceOf(NumberFormat.Blank.class, NumberFormat.blank());
        assertInstanceOf(NumberFormat.Default.class, NumberFormat.defaultFormat());
    }

    @Test
    void fixedCarriesItsComponent() {
        Component c = Component.text("12");
        NumberFormat fixed = NumberFormat.fixed(c);
        assertInstanceOf(NumberFormat.Fixed.class, fixed);
        assertEquals(c, ((NumberFormat.Fixed) fixed).component());
    }
}
