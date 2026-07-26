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

    public static @NotNull Component mini(@NotNull String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }
}
