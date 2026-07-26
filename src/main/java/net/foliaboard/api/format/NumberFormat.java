package net.foliaboard.api.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

public sealed interface NumberFormat {
    static @NotNull NumberFormat blank() {
        return Blank.INSTANCE;
    }

    static @NotNull NumberFormat fixed(@NotNull ComponentLike component) {
        return new Fixed(component.asComponent());
    }

    static @NotNull NumberFormat styled(@NotNull Style style) {
        return new Styled(style);
    }

    static @NotNull NumberFormat defaultFormat() {
        return Default.INSTANCE;
    }

    final class Blank implements NumberFormat {
        public static final Blank INSTANCE = new Blank();
        private Blank() {
        }
    }

    final class Default implements NumberFormat {
        public static final Default INSTANCE = new Default();
        private Default() {
        }
    }

    record Fixed(@NotNull Component component) implements NumberFormat {
    }

    record Styled(@NotNull Style style) implements NumberFormat {
    }
}
