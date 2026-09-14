package net.foliaboard.api.format;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

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
