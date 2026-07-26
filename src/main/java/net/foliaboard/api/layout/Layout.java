package net.foliaboard.api.layout;

import net.foliaboard.FoliaBoard;
import net.foliaboard.api.BoardBuilder;
import net.foliaboard.api.Sidebar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class Layout {
    private final String name;
    private final Consumer<BoardBuilder> recipe;

    private Layout(String name, Consumer<BoardBuilder> recipe) {
        this.name = name;
        this.recipe = recipe;
    }

    public static @NotNull Layout named(@NotNull String name, @NotNull Consumer<BoardBuilder> recipe) {
        return new Layout(name, recipe);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull Sidebar applyTo(@NotNull FoliaBoard board, @NotNull Player player) {
        BoardBuilder builder = board.createBoard(player);
        recipe.accept(builder);
        return builder.build();
    }
}
