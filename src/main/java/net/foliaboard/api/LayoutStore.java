package net.foliaboard.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LayoutStore {
    @NotNull CompletableFuture<Void> remember(@NotNull UUID player, @NotNull String layoutName);

    @NotNull CompletableFuture<@Nullable String> lastLayout(@NotNull UUID player);
}
