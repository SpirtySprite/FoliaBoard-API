package net.foliaboard.api.animation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void cycleAlwaysReturnsOneOfItsFrames() {
        Animation<Component> a = Animations.cycle(Duration.ofMillis(50),
                Component.text("A"), Component.text("B"), Component.text("C"));
        Set<String> frames = Set.of("A", "B", "C");
        for (int i = 0; i < 200; i++) {
            assertTrue(frames.contains(PLAIN.serialize(a.current())));
        }
    }

    @Test
    void scrollTextOutputAlwaysMatchesWindowWidth() {
        int width = 10;
        Animation<Component> a = Animations.scrollText(Duration.ofMillis(20), "hello world",
                width, net.kyori.adventure.text.format.NamedTextColor.WHITE);
        for (int i = 0; i < 100; i++) {
            assertEquals(width, PLAIN.serialize(a.current()).length());
        }
    }

    @Test
    void scrollTextDoesNotSplitEmoji() {
        Animation<Component> a = Animations.scrollText(Duration.ofMillis(20), "ab🔥cd", 4,
                net.kyori.adventure.text.format.NamedTextColor.WHITE);
        for (int i = 0; i < 100; i++) {
            String s = PLAIN.serialize(a.current());
            for (int c = 0; c < s.length(); c++) {
                char ch = s.charAt(c);
                if (Character.isHighSurrogate(ch)) {
                    assertTrue(c + 1 < s.length() && Character.isLowSurrogate(s.charAt(c + 1)),
                            "high surrogate without its pair: " + s);
                }
            }
        }
    }

    @Test
    void typewriterOnlyEverShowsAPrefix() {
        String text = "Loading";
        Animation<Component> a = Animations.typewriter(Duration.ofMillis(20), Component.text(text));
        for (int i = 0; i < 100; i++) {
            String shown = PLAIN.serialize(a.current());
            assertTrue(text.startsWith(shown), "typewriter showed a non-prefix: " + shown);
        }
    }

    @Test
    void gradientWaveKeepsTheText() {
        Animation<Component> a = Animations.gradientWave(Duration.ofMillis(400), "Nexus",
                net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE,
                net.kyori.adventure.text.format.NamedTextColor.AQUA);
        for (int i = 0; i < 50; i++) {
            assertEquals("Nexus", PLAIN.serialize(a.current()));
        }
    }

    @Test
    void framesParseLegacyAndMiniMessage() {
        Animation<Component> a = Animations.frames(Duration.ofMillis(10), "&aOne", "<red>One");
        assertEquals("One", PLAIN.serialize(a.current()));
    }

    @Test
    void blinkAlternatesBetweenShownAndEmpty() {
        Animation<Component> a = Animations.blink(Duration.ofMillis(5), Component.text("!"));
        Set<String> seen = new java.util.HashSet<>();
        long until = System.currentTimeMillis() + 200;
        while (System.currentTimeMillis() < until && seen.size() < 2) {
            seen.add(PLAIN.serialize(a.current()));
        }
        assertEquals(Set.of("!", ""), seen);
    }

    @Test
    void mapAndConstantCompose() {
        Animation<Integer> constant = Animation.constant(21);
        assertEquals(42, constant.map(value -> value * 2).current());
    }
}
