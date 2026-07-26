package net.foliaboard.internal.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Schedulers {
    private static final boolean FOLIA = detectFolia();

    private Schedulers() {
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private static volatile boolean synchronousForTesting = false;

    public static void setSynchronousForTesting(boolean value) {
        synchronousForTesting = value;
    }

    public static @NotNull ScheduledHandle globalTimer(@NotNull Plugin plugin, @NotNull Runnable task,
                                                       long delayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return () -> {
            };
        }
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        var handle = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period);
        return handle::cancel;
    }

    public static void async(@NotNull Plugin plugin, @NotNull Runnable task) {
        if (synchronousForTesting) {
            task.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    public static void global(@NotNull Plugin plugin, @NotNull Runnable task) {
        if (synchronousForTesting) {
            task.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    public static boolean onEntity(@NotNull Plugin plugin, @NotNull Entity entity,
                                   @NotNull Runnable task, @Nullable Runnable retired) {
        if (synchronousForTesting) {
            task.run();
            return true;
        }

        if (!plugin.isEnabled()) {
            return false;
        }
        try {
            return entity.getScheduler().run(plugin, scheduledTask -> task.run(),
                    retired == null ? null : retired) != null;
        } catch (IllegalPluginAccessException disabledMidCall) {
            return false;
        }
    }

    public static boolean onEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        return onEntity(plugin, entity, task, null);
    }

    public static @NotNull ScheduledHandle entityTimer(@NotNull Plugin plugin, @NotNull Entity entity,
                                                       @NotNull Consumer<ScheduledHandle> task,
                                                       long delayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return () -> {
            };
        }
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        try {
            var handle = entity.getScheduler().runAtFixedRate(plugin,
                    scheduledTask -> task.accept(scheduledTask::cancel), null, delay, period);
            if (handle == null) {
                return () -> {
                };
            }
            return handle::cancel;
        } catch (IllegalPluginAccessException disabledMidCall) {
            return () -> {
            };
        }
    }

    @FunctionalInterface
    public interface ScheduledHandle {
        void cancel();
    }
}
