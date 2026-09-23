package net.foliaboard.api.animation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Animations {
    private Animations() {
    }

    @SafeVarargs
    public static <T> @NotNull Animation<T> cycle(@NotNull Duration period, @NotNull T... frames) {
        return cycle(period, Arrays.asList(frames));
    }

    public static <T> @NotNull Animation<T> cycle(@NotNull Duration period, @NotNull List<T> frames) {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("frames must not be empty");
        }
        List<T> copy = new ArrayList<>(frames);
        long periodMs = Math.max(1, period.toMillis());
        return () -> copy.get((int) ((System.currentTimeMillis() / periodMs) % copy.size()));
    }

    public static @NotNull Animation<Component> scrollText(@NotNull Duration period, @NotNull String text,
                                                           int width, @NotNull TextColor color) {
        int[] cps = (text + "   ").codePoints().toArray();
        int len = cps.length;
        long periodMs = Math.max(1, period.toMillis());
        return () -> {
            int offset = (int) ((System.currentTimeMillis() / periodMs) % len);
            StringBuilder sb = new StringBuilder(width);
            for (int i = 0; i < width; i++) {
                sb.appendCodePoint(cps[(offset + i) % len]);
            }
            return Component.text(sb.toString(), color);
        };
    }

    public static @NotNull Animation<Component> pulseColor(@NotNull Duration period, @NotNull String text,
                                                           @NotNull TextColor from, @NotNull TextColor to) {
        long periodMs = Math.max(1, period.toMillis());
        return () -> {
            double phase = (System.currentTimeMillis() % periodMs) / (double) periodMs;
            double t = phase < 0.5 ? phase * 2 : (1 - phase) * 2;
            TextColor lerped = TextColor.lerp((float) t, from, to);
            return Component.text(text, lerped);
        };
    }

    public static @NotNull Animation<Component> typewriter(@NotNull Duration perChar, @NotNull TextComponent style) {
        int[] cps = style.content().codePoints().toArray();
        long perCharMs = Math.max(1, perChar.toMillis());
        int hold = 8;
        int total = cps.length + hold;
        return () -> {
            int step = (int) ((System.currentTimeMillis() / perCharMs) % total);
            int show = Math.min(cps.length, step + 1);
            String shown = new String(cps, 0, show);
            return Component.text(shown).style(style.style());
        };
    }

    public static @NotNull Animation<Component> frames(@NotNull Duration period, @NotNull String... anyFormat) {
        List<Component> parsed = new ArrayList<>(anyFormat.length);
        for (String frame : anyFormat) {
            parsed.add(net.foliaboard.api.text.Text.parse(frame));
        }
        return cycle(period, parsed);
    }

    public static @NotNull Animation<Component> gradientWave(@NotNull Duration period, @NotNull String text,
                                                             @NotNull TextColor... colors) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("a gradient needs at least two colors");
        }
        StringBuilder stops = new StringBuilder();
        for (TextColor color : colors) {
            stops.append(':').append(color.asHexString());
        }
        String escaped = net.foliaboard.api.text.Text.escape(text);
        long periodMs = Math.max(1, period.toMillis());
        int steps = 20;
        List<Component> frames = new ArrayList<>(steps);
        for (int step = 0; step < steps; step++) {
            double phase = -1.0D + 2.0D * step / steps;
            frames.add(MiniMessage.miniMessage().deserialize("<gradient" + stops + ":"
                    + String.format(java.util.Locale.ROOT, "%.2f", phase) + ">" + escaped + "</gradient>"));
        }
        long frameMs = Math.max(1, periodMs / steps);
        return () -> frames.get((int) ((System.currentTimeMillis() / frameMs) % steps));
    }

    public static @NotNull Animation<Component> blink(@NotNull Duration period, @NotNull Component shown) {
        long periodMs = Math.max(1, period.toMillis());
        return () -> (System.currentTimeMillis() / periodMs) % 2 == 0 ? shown : Component.empty();
    }

    public static <T> @NotNull Animation<T> sequence(@NotNull List<Animation<T>> animations, @NotNull Duration each) {
        if (animations.isEmpty()) {
            throw new IllegalArgumentException("animations must not be empty");
        }
        List<Animation<T>> copy = List.copyOf(animations);
        long eachMs = Math.max(1, each.toMillis());
        return () -> copy.get((int) ((System.currentTimeMillis() / eachMs) % copy.size())).current();
    }

    public static @NotNull Component mini(@NotNull String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }
}
