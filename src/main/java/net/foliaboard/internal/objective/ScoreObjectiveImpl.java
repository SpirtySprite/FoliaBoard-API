package net.foliaboard.internal.objective;

import net.foliaboard.api.ScoreObjective;
import net.foliaboard.internal.packet.DisplaySlotType;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreObjectiveImpl implements ScoreObjective {
    private final Plugin plugin;
    private final PacketAdapter adapter;
    private final String objectiveId;
    private final DisplaySlotType slot;

    private volatile Component title = Component.empty();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();

    private final Map<UUID, Map<String, Integer>> perViewer = new ConcurrentHashMap<>();
    private final Set<UUID> initialisedViewers = ConcurrentHashMap.newKeySet();
    private volatile boolean hidden = false;

    public ScoreObjectiveImpl(Plugin plugin, PacketAdapter adapter, String objectiveId, DisplaySlotType slot) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.objectiveId = objectiveId;
        this.slot = slot;
    }

    @Override
    public @NotNull ScoreObjective title(@NotNull ComponentLike title) {
        this.title = title.asComponent();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, viewer, () -> {
                if (initialisedViewers.contains(viewer.getUniqueId())) {
                    adapter.updateObjective(viewer, objectiveId, this.title);
                } else {
                    initViewer(viewer);
                }
            });
        }
        return this;
    }

    @Override
    public @NotNull ScoreObjective score(@NotNull Player target, int value) {
        return score(target.getName(), value);
    }

    @Override
    public @NotNull ScoreObjective score(@NotNull String entry, int value) {
        Integer previous = scores.put(entry, value);

        if (hidden || (previous != null && previous == value)) {
            return this;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, viewer, () -> {
                if (!initialisedViewers.contains(viewer.getUniqueId())) {
                    initViewer(viewer);
                } else {
                    adapter.setScore(viewer, objectiveId, entry, value, null, null);
                }
            });
        }
        return this;
    }

    @Override
    public @NotNull ScoreObjective remove(@NotNull String entry) {
        scores.remove(entry);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, viewer, () -> adapter.resetScore(viewer, objectiveId, entry));
        }
        return this;
    }

    @Override
    public @NotNull ScoreObjective scoreFor(@NotNull Player viewer, @NotNull String entry, int value) {
        Integer previous = perViewer.computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(entry, value);
        if (hidden || (previous != null && previous == value)) {
            return this;
        }
        Schedulers.onEntity(plugin, viewer, () -> {
            if (!initialisedViewers.contains(viewer.getUniqueId())) {
                initViewer(viewer);
            } else {
                adapter.setScore(viewer, objectiveId, entry, value, null, null);
            }
        });
        return this;
    }

    @Override
    public @NotNull ScoreObjective removeFor(@NotNull Player viewer, @NotNull String entry) {
        Map<String, Integer> overrides = perViewer.get(viewer.getUniqueId());
        if (overrides != null) {
            overrides.remove(entry);
        }
        Schedulers.onEntity(plugin, viewer, () -> {
            Integer shared = scores.get(entry);
            if (shared != null) {
                adapter.setScore(viewer, objectiveId, entry, shared, null, null);
            } else {
                adapter.resetScore(viewer, objectiveId, entry);
            }
        });
        return this;
    }

    @Override
    public void hide() {
        hidden = true;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, viewer, () -> {
                adapter.removeObjective(viewer, objectiveId);
                initialisedViewers.remove(viewer.getUniqueId());
            });
        }
    }

    @Override
    public void show() {
        if (!hidden) {
            return;
        }
        hidden = false;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, viewer, () -> initViewer(viewer));
        }
    }

    @Override
    public boolean hidden() {
        return hidden;
    }

    public void onJoin(@NotNull Player viewer) {
        if (hidden) {
            return;
        }
        Schedulers.onEntity(plugin, viewer, () -> initViewer(viewer));
    }

    public void onQuit(@NotNull Player viewer) {
        initialisedViewers.remove(viewer.getUniqueId());
        perViewer.remove(viewer.getUniqueId());

        if (scores.remove(viewer.getName()) != null) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(viewer.getUniqueId())) {
                    continue;
                }
                Schedulers.onEntity(plugin, other, () -> adapter.resetScore(other, objectiveId, viewer.getName()));
            }
        }
    }

    private void initViewer(Player viewer) {
        if (hidden || !viewer.isOnline()) {
            return;
        }

        if (!initialisedViewers.add(viewer.getUniqueId())) {
            return;
        }
        adapter.createObjective(viewer, objectiveId, title);
        adapter.setDisplaySlot(viewer, objectiveId, slot);

        Map<String, Integer> snapshot = new LinkedHashMap<>(scores);
        Map<String, Integer> overrides = perViewer.get(viewer.getUniqueId());
        if (overrides != null) {
            snapshot.putAll(overrides);
        }
        for (Map.Entry<String, Integer> e : snapshot.entrySet()) {
            adapter.setScore(viewer, objectiveId, e.getKey(), e.getValue(), null, null);
        }
    }

    public void closeAll() {
        hide();
    }
}
