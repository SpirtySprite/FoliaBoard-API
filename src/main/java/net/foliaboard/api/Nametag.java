package net.foliaboard.api;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Nametag {
    @NotNull Player target();

    @NotNull Nametag prefix(@NotNull ComponentLike prefix);

    @NotNull Nametag suffix(@NotNull ComponentLike suffix);

    @NotNull Nametag color(@Nullable NamedTextColor color);

    @NotNull Nametag nametagVisibility(@NotNull Visibility visibility);

    @NotNull Nametag collision(@NotNull Collision collision);

    @NotNull Nametag perViewer(@Nullable NametagResolver resolver);

    @NotNull Nametag apply();

    void remove();

    enum Visibility {ALWAYS, NEVER, HIDE_FOR_OTHER_TEAMS, HIDE_FOR_OWN_TEAM}

    enum Collision {ALWAYS, NEVER, PUSH_OTHER_TEAMS, PUSH_OWN_TEAM}
}
