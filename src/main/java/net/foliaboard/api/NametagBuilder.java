package net.foliaboard.api;

import net.foliaboard.FoliaBoard;
import net.foliaboard.api.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NametagBuilder {
    private final FoliaBoard board;
    private final Player target;

    private Component prefix = Component.empty();
    private Component suffix = Component.empty();
    private @Nullable NamedTextColor color;
    private Nametag.Visibility visibility = Nametag.Visibility.ALWAYS;
    private Nametag.Collision collision = Nametag.Collision.ALWAYS;
    private @Nullable Integer tabSort;
    private @Nullable NametagResolver viewerResolver;

    public NametagBuilder(@NotNull FoliaBoard board, @NotNull Player target) {
        this.board = board;
        this.target = target;
    }

    public @NotNull NametagBuilder prefix(@NotNull String miniMessage) {
        this.prefix = Text.mini(miniMessage);
        return this;
    }

    public @NotNull NametagBuilder prefix(@NotNull ComponentLike prefix) {
        this.prefix = prefix.asComponent();
        return this;
    }

    public @NotNull NametagBuilder suffix(@NotNull String miniMessage) {
        this.suffix = Text.mini(miniMessage);
        return this;
    }

    public @NotNull NametagBuilder suffix(@NotNull ComponentLike suffix) {
        this.suffix = suffix.asComponent();
        return this;
    }

    public @NotNull NametagBuilder color(@Nullable NamedTextColor color) {
        this.color = color;
        return this;
    }

    public @NotNull NametagBuilder nametagVisibility(@NotNull Nametag.Visibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public @NotNull NametagBuilder collision(@NotNull Nametag.Collision collision) {
        this.collision = collision;
        return this;
    }

    public @NotNull NametagBuilder tabSort(int weight) {
        this.tabSort = weight;
        return this;
    }

    public @NotNull NametagBuilder perViewer(@NotNull NametagResolver resolver) {
        this.viewerResolver = resolver;
        return this;
    }

    public @NotNull Nametag apply() {
        Nametag nametag = board.nametagInternal(target, tabSort);
        nametag.prefix(prefix)
                .suffix(suffix)
                .color(color)
                .nametagVisibility(visibility)
                .collision(collision)
                .perViewer(viewerResolver)
                .apply();
        return nametag;
    }
}
