package net.foliaboard.internal.nametag;

import net.foliaboard.api.Nametag;
import net.foliaboard.internal.packet.PacketAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class NametagManager {
    private final Plugin plugin;
    private final PacketAdapter adapter;
    private final Map<UUID, NametagImpl> byTarget = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final Supplier<Collection<? extends Player>> online = Bukkit::getOnlinePlayers;

    public NametagManager(Plugin plugin, PacketAdapter adapter) {
        this.plugin = plugin;
        this.adapter = adapter;
    }

    public @NotNull Nametag get(@NotNull Player target) {
        return get(target, null, true);
    }

    public @NotNull Nametag get(@NotNull Player target, @Nullable Integer sortWeight, boolean applyNow) {
        NametagImpl existing = byTarget.get(target.getUniqueId());
        if (existing != null && !existing.removed()) {
            return existing;
        }
        NametagImpl impl = new NametagImpl(plugin, adapter, target, generateTeamName(sortWeight), online);
        byTarget.put(target.getUniqueId(), impl);
        if (applyNow) {
            impl.apply();
        }
        return impl;
    }

    private String generateTeamName(@Nullable Integer sortWeight) {
        String unique = Integer.toHexString(counter.getAndIncrement());
        String name = sortWeight != null
                ? String.format("%04d", Math.max(0, Math.min(9999, sortWeight))) + unique
                : "fbn" + unique;
        if (name.length() > 16) {
            throw new IllegalStateException("FoliaBoard: generated team name exceeds 16 chars: " + name);
        }
        return name;
    }

    public @Nullable Nametag getIfPresent(@NotNull Player target) {
        NametagImpl impl = byTarget.get(target.getUniqueId());
        return impl == null || impl.removed() ? null : impl;
    }

    public void onJoin(@NotNull Player viewer) {
        for (NametagImpl impl : byTarget.values()) {
            if (!impl.removed()) {
                impl.applyTo(viewer);
            }
        }
    }

    public void onQuit(@NotNull Player player) {
        UUID id = player.getUniqueId();
        NametagImpl own = byTarget.remove(id);
        if (own != null) {
            own.remove();
        }
        for (NametagImpl impl : byTarget.values()) {
            impl.forgetViewer(id);
        }
    }

    public int active() {
        int n = 0;
        for (NametagImpl impl : byTarget.values()) {
            if (!impl.removed()) {
                n++;
            }
        }
        return n;
    }

    public void closeAll() {
        for (NametagImpl impl : byTarget.values()) {
            impl.remove();
        }
        byTarget.clear();
    }
}
