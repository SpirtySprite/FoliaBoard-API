package net.foliaboard.internal.nametag;

import net.foliaboard.api.Nametag;
import net.foliaboard.api.NametagResolver;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.packet.TeamData;
import net.foliaboard.internal.scheduler.Schedulers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class NametagImpl implements Nametag {
    private final Plugin plugin;
    private final PacketAdapter adapter;
    private final Player target;
    private final String teamName;
    private final Supplier<Collection<? extends Player>> onlinePlayers;
    private final TeamData data;
    private final Set<UUID> receivers = ConcurrentHashMap.newKeySet();
    private volatile boolean removed = false;
    private volatile NametagResolver viewerResolver;

    public NametagImpl(Plugin plugin, PacketAdapter adapter, Player target, String teamName,
                       Supplier<Collection<? extends Player>> onlinePlayers) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.target = target;
        this.teamName = teamName;
        this.onlinePlayers = onlinePlayers;
        this.data = new TeamData(teamName);
    }

    @Override
    public @NotNull Player target() {
        return target;
    }

    @Override
    public synchronized @NotNull Nametag prefix(@NotNull ComponentLike prefix) {
        data.prefix(prefix.asComponent());
        return this;
    }

    @Override
    public synchronized @NotNull Nametag suffix(@NotNull ComponentLike suffix) {
        data.suffix(suffix.asComponent());
        return this;
    }

    @Override
    public synchronized @NotNull Nametag color(@Nullable NamedTextColor color) {
        data.color(color);
        return this;
    }

    @Override
    public synchronized @NotNull Nametag nametagVisibility(@NotNull Visibility visibility) {
        data.nametagVisibility(TeamData.Visibility.valueOf(visibility.name()));
        return this;
    }

    @Override
    public synchronized @NotNull Nametag collision(@NotNull Collision collision) {
        data.collision(TeamData.Collision.valueOf(collision.name()));
        return this;
    }

    @Override
    public @NotNull Nametag perViewer(@Nullable NametagResolver resolver) {
        this.viewerResolver = resolver;
        return this;
    }

    @Override
    public @NotNull Nametag apply() {
        if (removed) {
            return this;
        }
        for (Player viewer : onlinePlayers.get()) {
            applyTo(viewer);
        }
        return this;
    }

    public void applyTo(Player viewer) {
        if (removed || !viewer.isOnline()) {
            return;
        }
        UUID id = viewer.getUniqueId();
        Schedulers.onEntity(plugin, viewer, () -> {
            if (removed) {
                return;
            }
            TeamData toSend = snapshotFor(viewer);
            if (receivers.add(id)) {
                adapter.createTeam(viewer, toSend, List.of(target.getName()));
            } else {
                adapter.updateTeam(viewer, toSend);
                adapter.teamEntries(viewer, teamName, List.of(target.getName()), true);
            }
        });
    }

    private TeamData snapshotFor(Player viewer) {
        TeamData snapshot;
        synchronized (this) {
            snapshot = data.copy();
        }
        NametagResolver resolver = viewerResolver;
        if (resolver != null) {
            resolver.resolve(viewer, target, new NametagStyleImpl(snapshot));
        }
        return snapshot;
    }

    public void forgetViewer(UUID viewer) {
        receivers.remove(viewer);
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;
        for (Player viewer : onlinePlayers.get()) {
            Schedulers.onEntity(plugin, viewer, () -> adapter.removeTeam(viewer, teamName));
        }
        receivers.clear();
    }

    public boolean removed() {
        return removed;
    }

    public String teamName() {
        return teamName;
    }
}
