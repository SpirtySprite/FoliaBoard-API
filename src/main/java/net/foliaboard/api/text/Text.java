package net.foliaboard.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CACHE_LIMIT = 2048;
    private static final Map<String, Component> CACHE = new ConcurrentHashMap<>();

    private Text() {
    }

    public static @NotNull MiniMessage miniMessage() {
        return MM;
    }

    public static @NotNull Component mini(@NotNull String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public static @NotNull Component mini(@NotNull String miniMessage, @NotNull TagResolver... resolvers) {
        return MM.deserialize(miniMessage, resolvers);
    }

    public static @NotNull Component parse(@NotNull String anyFormat) {
        return MM.deserialize(Legacy.toMini(anyFormat));
    }

    public static @NotNull Component cached(@NotNull String anyFormat) {
        Component hit = CACHE.get(anyFormat);
        if (hit != null) {
            return hit;
        }
        Component parsed = parse(anyFormat);
        if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(anyFormat, parsed);
        return parsed;
    }

    public static @NotNull String toMini(@NotNull Component component) {
        return MM.serialize(component);
    }

    public static @NotNull String plain(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static @NotNull String escape(@NotNull String value) {
        if (value.indexOf('<') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '<' || character == '\\') {
                escaped.append('\\');
            }
            escaped.append(character);
        }
        return escaped.toString();
    }

    public static @NotNull Component empty() {
        return Component.empty();
    }
}
