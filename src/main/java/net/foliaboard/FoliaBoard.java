package net.foliaboard;

import net.foliaboard.api.BoardBuilder;
import net.foliaboard.api.FoliaBoardStats;
import net.foliaboard.api.Nametag;
import net.foliaboard.api.NametagBuilder;
import net.foliaboard.api.ScoreObjective;
import net.foliaboard.api.Sidebar;
import net.foliaboard.api.SidebarProvider;
import net.foliaboard.api.event.LayoutApplyEvent;
import net.foliaboard.api.event.SidebarCreateEvent;
import net.foliaboard.api.hook.LineProcessor;
import net.foliaboard.api.layout.Layout;
import net.foliaboard.api.placeholder.Placeholders;
import net.foliaboard.internal.board.SidebarImpl;
import net.foliaboard.internal.listener.FoliaBoardListener;
import net.foliaboard.internal.metrics.PacketMetrics;
import net.foliaboard.internal.nametag.NametagManager;
import net.foliaboard.internal.objective.ScoreObjectiveImpl;
import net.foliaboard.internal.packet.DisplaySlotType;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.packet.PacketAdapterFactory;
import net.foliaboard.internal.scheduler.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// Entry point: create one in onEnable, close() in onDisable. All methods are thread-safe.
public final class FoliaBoard {
    private final Plugin plugin;
    private final PacketAdapter adapter;
    private final Placeholders placeholders = new Placeholders();

    private final Map<UUID, SidebarImpl> sidebars = new ConcurrentHashMap<>();
    private final Map<UUID, Schedulers.ScheduledHandle> refreshHandles = new ConcurrentHashMap<>();
    private final NametagManager nametags;
    private final AtomicInteger objectiveCounter = new AtomicInteger();

    private final List<LineProcessor> lineProcessors = new CopyOnWriteArrayList<>();
    private final Map<String, Layout> layouts = new ConcurrentHashMap<>();
    private final Map<String, String> worldLayouts = new ConcurrentHashMap<>();

    private final Set<UUID> builderOwned = ConcurrentHashMap.newKeySet();

    private final Set<UUID> worldLayoutOwned = ConcurrentHashMap.newKeySet();

    private volatile ScoreObjectiveImpl belowName;
    private volatile ScoreObjectiveImpl tabList;

    private volatile SidebarProvider globalProvider;
    private volatile Schedulers.ScheduledHandle providerTask;
    private volatile Layout globalLayout;
    private volatile net.foliaboard.api.LayoutStore layoutStore;

    private final FoliaBoardListener listener;
    private final PacketMetrics metrics = new PacketMetrics();
    private volatile boolean closed = false;

    private FoliaBoard(Plugin plugin, PacketAdapter adapter) {
        this.plugin = plugin;
        this.adapter = adapter;
        adapter.attachMetrics(metrics);
        this.nametags = new NametagManager(plugin, adapter);
        this.listener = new FoliaBoardListener(this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public @NotNull FoliaBoardStats stats() {
        return new FoliaBoardStats(
                metrics.totalPackets(), metrics.refreshCount(), sidebars.size(), nametags.active());
    }

    public void recordRefresh() {
        metrics.refresh();
    }

    public static @NotNull FoliaBoard create(@NotNull Plugin plugin) {
        PacketAdapter adapter = PacketAdapterFactory.create(plugin.getLogger());
        plugin.getLogger().info("FoliaBoard ready (" + (Schedulers.isFolia() ? "Folia" : "Paper") + " scheduling).");
        return new FoliaBoard(plugin, adapter);
    }

    public @NotNull BoardBuilder createBoard(@NotNull Player player) {
        ensureOpen();
        return new BoardBuilder(this, player);
    }

    public @NotNull NametagBuilder createNametag(@NotNull Player player) {
        ensureOpen();
        return new NametagBuilder(this, player);
    }

    public @NotNull Sidebar sidebar(@NotNull Player player) {
        ensureOpen();
        boolean[] created = {false};
        SidebarImpl sidebar = sidebars.computeIfAbsent(player.getUniqueId(), id -> {
            created[0] = true;
            return new SidebarImpl(plugin, adapter, player, nextObjectiveId(), lineProcessors);
        });
        if (created[0]) {
            Schedulers.onEntity(plugin, player,
                    () -> Bukkit.getPluginManager().callEvent(new SidebarCreateEvent(player, sidebar)));
        }
        return sidebar;
    }

    public @Nullable Sidebar sidebarIfPresent(@NotNull Player player) {
        return sidebars.get(player.getUniqueId());
    }

    public void removeSidebar(@NotNull Player player) {
        UUID id = player.getUniqueId();
        cancelRefresh(id);
        builderOwned.remove(id);
        worldLayoutOwned.remove(id);
        SidebarImpl removed = sidebars.remove(id);
        if (removed != null) {
            removed.close();
        }
    }

    public void markBuilderOwned(@NotNull Player player) {
        builderOwned.add(player.getUniqueId());
    }

    public void setGlobalSidebar(@NotNull Layout layout) {
        ensureOpen();
        registerLayout(layout);
        clearGlobalSidebar();
        this.globalLayout = layout;
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyLayout(player, layout);
        }
    }

    public void setGlobalSidebar(@NotNull SidebarProvider provider) {
        ensureOpen();
        this.globalLayout = null;
        this.globalProvider = provider;
        Schedulers.ScheduledHandle old = this.providerTask;
        if (old != null) {
            old.cancel();
        }
        int interval = Math.max(1, provider.refreshIntervalTicks());
        this.providerTask = Schedulers.globalTimer(plugin, () -> {
            SidebarProvider current = this.globalProvider;
            if (current == null) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                Schedulers.onEntity(plugin, player, () -> refreshFromProvider(player, current));
            }
        }, interval, interval);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Schedulers.onEntity(plugin, player, () -> refreshFromProvider(player, provider));
        }
    }

    public void clearGlobalSidebar() {
        this.globalProvider = null;
        Schedulers.ScheduledHandle task = this.providerTask;
        if (task != null) {
            task.cancel();
            this.providerTask = null;
        }
    }

    private void refreshFromProvider(Player player, SidebarProvider provider) {
        if (closed || !player.isOnline()) {
            return;
        }
        metrics.refresh();

        if (builderOwned.contains(player.getUniqueId())) {
            return;
        }
        Sidebar sidebar = sidebar(player);
        if (!provider.visible(player)) {
            sidebar.visible(false);
            return;
        }
        sidebar.visible(true);
        sidebar.title(provider.title(player));
        sidebar.lines(provider.lines(player));
    }

    public @NotNull FoliaBoard registerLayout(@NotNull Layout layout) {
        layouts.put(layout.name().toLowerCase(), layout);
        return this;
    }

    public @Nullable Layout layout(@NotNull String name) {
        return layouts.get(name.toLowerCase());
    }

    public @NotNull FoliaBoard unregisterLayout(@NotNull String name) {
        layouts.remove(name.toLowerCase());
        return this;
    }

    public @NotNull Sidebar applyLayout(@NotNull Player player, @NotNull String layoutName) {
        Layout layout = layout(layoutName);
        if (layout == null) {
            throw new IllegalArgumentException("No layout registered named '" + layoutName + "'");
        }
        return applyLayout(player, layout);
    }

    public @NotNull Sidebar applyLayout(@NotNull Player player, @NotNull Layout layout) {
        ensureOpen();
        markBuilderOwned(player);
        Sidebar sidebar = sidebar(player);
        Schedulers.onEntity(plugin, player, () -> {
            LayoutApplyEvent event = new LayoutApplyEvent(player, layout);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                event.getLayout().applyTo(this, player);
                net.foliaboard.api.LayoutStore store = layoutStore;
                if (store != null) {
                    UUID id = player.getUniqueId();
                    String name = event.getLayout().name();
                    Schedulers.async(plugin, () -> store.remember(id, name));
                }
            }
        });
        return sidebar;
    }

    public @NotNull FoliaBoard setLayoutStore(@NotNull net.foliaboard.api.LayoutStore store) {
        this.layoutStore = store;
        return this;
    }

    public @NotNull FoliaBoard setWorldLayout(@NotNull String worldName, @NotNull String layoutName) {
        worldLayouts.put(worldName, layoutName);
        return this;
    }

    public @NotNull FoliaBoard clearWorldLayout(@NotNull String worldName) {
        worldLayouts.remove(worldName);
        return this;
    }

    private boolean applyWorldLayoutIfAny(Player player) {
        UUID id = player.getUniqueId();
        String layoutName = worldLayouts.get(player.getWorld().getName());
        if (layoutName != null) {
            Layout layout = layout(layoutName);
            if (layout != null) {
                applyLayout(player, layout);
                worldLayoutOwned.add(id);
                return true;
            }
        } else if (worldLayoutOwned.remove(id)) {
            removeSidebar(player);
        }
        return false;
    }

    public @NotNull Nametag nametag(@NotNull Player target) {
        ensureOpen();
        return nametags.get(target);
    }

    public @NotNull Nametag nametagInternal(@NotNull Player target, @Nullable Integer sortWeight) {
        ensureOpen();
        return nametags.get(target, sortWeight, false);
    }

    public @Nullable Nametag nametagIfPresent(@NotNull Player target) {
        return nametags.getIfPresent(target);
    }

    public @NotNull ScoreObjective belowName() {
        ensureOpen();
        ScoreObjectiveImpl local = belowName;
        if (local == null) {
            synchronized (this) {
                if (belowName == null) {
                    belowName = new ScoreObjectiveImpl(plugin, adapter, "fb_bn", DisplaySlotType.BELOW_NAME);
                }
                local = belowName;
            }
        }
        return local;
    }

    public @NotNull ScoreObjective tabList() {
        ensureOpen();
        ScoreObjectiveImpl local = tabList;
        if (local == null) {
            synchronized (this) {
                if (tabList == null) {
                    tabList = new ScoreObjectiveImpl(plugin, adapter, "fb_tab", DisplaySlotType.PLAYER_LIST);
                }
                local = tabList;
            }
        }
        return local;
    }

    public void tabName(@NotNull Player target, @NotNull String miniMessage) {
        tabName(target, net.foliaboard.api.text.Text.mini(miniMessage));
    }

    public void tabName(@NotNull Player target, @NotNull net.kyori.adventure.text.ComponentLike name) {
        net.kyori.adventure.text.Component c = name.asComponent();
        Schedulers.onEntity(plugin, target, () -> target.playerListName(c));
    }

    public void resetTabName(@NotNull Player target) {
        Schedulers.onEntity(plugin, target, () -> target.playerListName(null));
    }

    public void tabOrder(@NotNull Player target, int order) {
        Schedulers.onEntity(plugin, target, () -> net.foliaboard.internal.tab.TabOrder.set(target, order));
    }

    public boolean tabOrderSupported() {
        return net.foliaboard.internal.tab.TabOrder.supported();
    }

    public void tabNameFor(@NotNull Player viewer, @NotNull Player target, @NotNull String miniMessage) {
        tabNameFor(viewer, target, net.foliaboard.api.text.Text.mini(miniMessage));
    }

    public void tabNameFor(@NotNull Player viewer, @NotNull Player target,
                           @NotNull net.kyori.adventure.text.ComponentLike name) {
        net.kyori.adventure.text.Component c = name.asComponent();
        Schedulers.onEntity(plugin, viewer, () -> adapter.tabDisplayName(viewer, target, c));
    }

    public void resetTabNameFor(@NotNull Player viewer, @NotNull Player target) {
        Schedulers.onEntity(plugin, viewer, () -> adapter.tabDisplayName(viewer, target, null));
    }

    public boolean perViewerTabSupported() {
        return adapter.supportsPerViewerTab();
    }

    public void tabHeaderFooter(@NotNull Player player, @NotNull String header, @NotNull String footer) {
        tabHeaderFooter(player, net.foliaboard.api.text.Text.mini(header), net.foliaboard.api.text.Text.mini(footer));
    }

    public void tabHeaderFooter(@NotNull Player player,
                                @NotNull net.kyori.adventure.text.ComponentLike header,
                                @NotNull net.kyori.adventure.text.ComponentLike footer) {
        var h = header.asComponent();
        var f = footer.asComponent();
        Schedulers.onEntity(plugin, player, () -> player.sendPlayerListHeaderAndFooter(h, f));
    }

    public void clearTabHeaderFooter(@NotNull Player player) {
        Schedulers.onEntity(plugin, player,
                () -> player.sendPlayerListHeaderAndFooter(net.kyori.adventure.text.Component.empty(),
                        net.kyori.adventure.text.Component.empty()));
    }

    public @NotNull FoliaBoard addLineProcessor(@NotNull LineProcessor processor) {
        lineProcessors.add(processor);
        return this;
    }

    public @NotNull FoliaBoard removeLineProcessor(@NotNull LineProcessor processor) {
        lineProcessors.remove(processor);
        return this;
    }

    public @NotNull Placeholders placeholders() {
        return placeholders;
    }

    public @NotNull Plugin plugin() {
        return plugin;
    }

    public void trackRefresh(@NotNull Player player, @Nullable Schedulers.ScheduledHandle handle) {
        Schedulers.ScheduledHandle previous = handle == null
                ? refreshHandles.remove(player.getUniqueId())
                : refreshHandles.put(player.getUniqueId(), handle);
        if (previous != null) {
            previous.cancel();
        }
    }

    private void cancelRefresh(UUID id) {
        Schedulers.ScheduledHandle handle = refreshHandles.remove(id);
        if (handle != null) {
            handle.cancel();
        }
    }

    public void handleJoin(@NotNull Player player) {
        if (closed) {
            return;
        }
        nametags.onJoin(player);
        if (belowName != null) {
            belowName.onJoin(player);
        }
        if (tabList != null) {
            tabList.onJoin(player);
        }
        SidebarProvider provider = globalProvider;
        if (provider != null) {
            Schedulers.onEntity(plugin, player, () -> refreshFromProvider(player, provider));
        }
        Layout global = globalLayout;
        if (global != null) {
            applyLayout(player, global);
        }
        boolean worldLayoutApplied = applyWorldLayoutIfAny(player);

        net.foliaboard.api.LayoutStore store = layoutStore;
        if (store != null && global == null && !worldLayoutApplied) {
            store.lastLayout(player.getUniqueId()).thenAccept(name -> {
                if (name != null && !closed && player.isOnline() && layout(name) != null) {
                    applyLayout(player, name);
                }
            });
        }
    }

    public void handleWorldChange(@NotNull Player player) {
        if (closed) {
            return;
        }
        applyWorldLayoutIfAny(player);
    }

    public void handleQuit(@NotNull Player player) {
        removeSidebar(player);
        nametags.onQuit(player);
        if (belowName != null) {
            belowName.onQuit(player);
        }
        if (tabList != null) {
            tabList.onQuit(player);
        }
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        clearGlobalSidebar();
        HandlerList.unregisterAll(listener);
        for (Schedulers.ScheduledHandle handle : refreshHandles.values()) {
            handle.cancel();
        }
        refreshHandles.clear();
        for (SidebarImpl sidebar : sidebars.values()) {
            sidebar.close();
        }
        sidebars.clear();
        nametags.closeAll();
        if (belowName != null) {
            belowName.closeAll();
        }
        if (tabList != null) {
            tabList.closeAll();
        }
    }

    private String nextObjectiveId() {
        return "fb" + Integer.toHexString(objectiveCounter.getAndIncrement());
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("FoliaBoard has been closed");
        }
    }
}
