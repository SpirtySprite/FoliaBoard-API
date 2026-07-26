package net.foliaboard.api;

import net.foliaboard.FoliaBoard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ScoreboardAPI {
    private static volatile FoliaBoard instance;

    private ScoreboardAPI() {
    }

    public static @NotNull FoliaBoard init(@NotNull Plugin plugin) {
        FoliaBoard board = FoliaBoard.create(plugin);
        instance = board;
        return board;
    }

    public static @NotNull FoliaBoard get() {
        FoliaBoard local = instance;
        if (local == null) {
            throw new IllegalStateException("ScoreboardAPI.init(plugin) has not been called yet");
        }
        return local;
    }

    public static boolean isInitialised() {
        return instance != null;
    }

    public static void shutdown() {
        FoliaBoard local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }

    public static @NotNull BoardBuilder createBoard(@NotNull Player player) {
        return get().createBoard(player);
    }

    public static @NotNull NametagBuilder createNametag(@NotNull Player player) {
        return get().createNametag(player);
    }

    public static @NotNull Sidebar sidebar(@NotNull Player player) {
        return get().sidebar(player);
    }
}
