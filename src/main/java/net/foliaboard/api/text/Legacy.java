package net.foliaboard.api.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Legacy {
    private static final int SHORT_HEX_LENGTH = 8;
    private static final int EXPANDED_HEX_LENGTH = 14;

    private Legacy() {
    }

    public static boolean hasCodes(@Nullable String input) {
        if (input == null) {
            return false;
        }
        for (int index = 0; index + 1 < input.length(); index++) {
            char marker = input.charAt(index);
            if (marker(marker) && (tag(Character.toLowerCase(input.charAt(index + 1))) != null
                    || input.charAt(index + 1) == '#' || Character.toLowerCase(input.charAt(index + 1)) == 'x')) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull String toMini(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        StringBuilder rendered = new StringBuilder(input.length() + 16);
        int index = 0;
        while (index < input.length()) {
            index = append(input, index, rendered);
        }
        return rendered.toString();
    }

    public static @NotNull String strip(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        StringBuilder stripped = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            int consumed = codeLength(input, index);
            if (consumed > 0) {
                index += consumed;
            } else {
                stripped.append(input.charAt(index));
                index++;
            }
        }
        return stripped.toString();
    }

    private static int append(String input, int index, StringBuilder rendered) {
        char marker = input.charAt(index);
        if (!marker(marker) || index + 1 >= input.length()) {
            rendered.append(marker);
            return index + 1;
        }
        char next = input.charAt(index + 1);
        if (next == '#' && hasHex(input, index + 2, 6)) {
            rendered.append("<reset><#").append(input, index + 2, index + SHORT_HEX_LENGTH).append('>');
            return index + SHORT_HEX_LENGTH;
        }
        if (Character.toLowerCase(next) == 'x' && hasExpandedHex(input, index, marker)) {
            rendered.append("<reset><#");
            for (int digit = 0; digit < 6; digit++) {
                rendered.append(input.charAt(index + 3 + digit * 2));
            }
            rendered.append('>');
            return index + EXPANDED_HEX_LENGTH;
        }
        char code = Character.toLowerCase(next);
        String tag = tag(code);
        if (tag == null) {
            rendered.append(marker);
            return index + 1;
        }
        if (color(code)) {
            rendered.append("<reset>");
        }
        rendered.append('<').append(tag).append('>');
        return index + 2;
    }

    private static int codeLength(String input, int index) {
        char marker = input.charAt(index);
        if (!marker(marker) || index + 1 >= input.length()) {
            return 0;
        }
        char next = input.charAt(index + 1);
        if (next == '#' && hasHex(input, index + 2, 6)) {
            return SHORT_HEX_LENGTH;
        }
        if (Character.toLowerCase(next) == 'x' && hasExpandedHex(input, index, marker)) {
            return EXPANDED_HEX_LENGTH;
        }
        return tag(Character.toLowerCase(next)) == null ? 0 : 2;
    }

    private static boolean hasExpandedHex(String input, int index, char marker) {
        if (index + EXPANDED_HEX_LENGTH > input.length()) {
            return false;
        }
        for (int digit = 0; digit < 6; digit++) {
            int markerIndex = index + 2 + digit * 2;
            if (input.charAt(markerIndex) != marker || !hex(input.charAt(markerIndex + 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasHex(String input, int start, int length) {
        if (start + length > input.length()) {
            return false;
        }
        for (int index = start; index < start + length; index++) {
            if (!hex(input.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean marker(char value) {
        return value == '&' || value == '§';
    }

    private static boolean hex(char value) {
        return Character.digit(value, 16) >= 0;
    }

    private static boolean color(char code) {
        return code >= '0' && code <= '9' || code >= 'a' && code <= 'f';
    }

    private static String tag(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }
}
