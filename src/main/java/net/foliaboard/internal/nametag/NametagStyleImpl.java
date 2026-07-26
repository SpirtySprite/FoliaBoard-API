package net.foliaboard.internal.nametag;

import net.foliaboard.api.Nametag;
import net.foliaboard.api.NametagStyle;
import net.foliaboard.api.text.Text;
import net.foliaboard.internal.packet.TeamData;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class NametagStyleImpl implements NametagStyle {
    final TeamData data;

    NametagStyleImpl(TeamData base) {
        this.data = base;
    }

    @Override
    public @NotNull NametagStyle prefix(@NotNull String miniMessage) {
        data.prefix(Text.mini(miniMessage));
        return this;
    }

    @Override
    public @NotNull NametagStyle prefix(@NotNull ComponentLike prefix) {
        data.prefix(prefix.asComponent());
        return this;
    }

    @Override
    public @NotNull NametagStyle suffix(@NotNull String miniMessage) {
        data.suffix(Text.mini(miniMessage));
        return this;
    }

    @Override
    public @NotNull NametagStyle suffix(@NotNull ComponentLike suffix) {
        data.suffix(suffix.asComponent());
        return this;
    }

    @Override
    public @NotNull NametagStyle color(@Nullable NamedTextColor color) {
        data.color(color);
        return this;
    }

    @Override
    public @NotNull NametagStyle nametagVisibility(@NotNull Nametag.Visibility visibility) {
        data.nametagVisibility(TeamData.Visibility.valueOf(visibility.name()));
        return this;
    }

    @Override
    public @NotNull NametagStyle collision(@NotNull Nametag.Collision collision) {
        data.collision(TeamData.Collision.valueOf(collision.name()));
        return this;
    }
}
