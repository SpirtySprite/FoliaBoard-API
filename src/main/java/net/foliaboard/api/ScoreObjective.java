package net.foliaboard.api;

import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ScoreObjective {
    @NotNull ScoreObjective title(@NotNull ComponentLike title);

    @NotNull ScoreObjective score(@NotNull Player target, int value);

    @NotNull ScoreObjective score(@NotNull String entry, int value);

    @NotNull ScoreObjective remove(@NotNull String entry);

    @NotNull ScoreObjective scoreFor(@NotNull Player viewer, @NotNull String entry, int value);

    @NotNull ScoreObjective removeFor(@NotNull Player viewer, @NotNull String entry);

    void hide();

    void show();

    boolean hidden();
}
