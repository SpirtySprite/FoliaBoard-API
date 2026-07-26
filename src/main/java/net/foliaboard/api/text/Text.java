package net.foliaboard.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

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

    public static @NotNull String toMini(@NotNull Component component) {
        return MM.serialize(component);
    }

    public static @NotNull Component empty() {
        return Component.empty();
    }
}
