package net.foliaboard.api.async;

import net.foliaboard.internal.scheduler.Schedulers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class AsyncUtil {
    private AsyncUtil() {
    }

    public static void async(@NotNull Plugin plugin, @NotNull Runnable task) {
        Schedulers.async(plugin, task);
    }

    public static void asyncLater(@NotNull Plugin plugin, @NotNull Runnable task, @NotNull Duration delay) {
        Schedulers.asyncLater(plugin, task, delay);
    }

    public static void onPlayer(@NotNull Plugin plugin, @NotNull Player player, @NotNull Runnable task) {
        Schedulers.onEntity(plugin, player, task);
    }

    public static void global(@NotNull Plugin plugin, @NotNull Runnable task) {
        Schedulers.global(plugin, task);
    }

    public static boolean isFolia() {
        return Schedulers.isFolia();
    }
}
