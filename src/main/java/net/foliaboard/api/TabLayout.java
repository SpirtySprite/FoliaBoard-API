package net.foliaboard.api;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class TabLayout {
    private final Consumer<TabBuilder> recipe;

    private TabLayout(Consumer<TabBuilder> recipe) {
        this.recipe = recipe;
    }

    public static @NotNull TabLayout of(@NotNull Consumer<TabBuilder> recipe) {
        return new TabLayout(recipe);
    }

    public @NotNull TabBuilder applyTo(@NotNull TabBuilder builder) {
        recipe.accept(builder);
        return builder;
    }
}
