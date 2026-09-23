package net.foliaboard.api;

import net.foliaboard.FoliaBoard;
import net.foliaboard.internal.tab.TabImpl;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class TabBuilder {
    private final FoliaBoard board;
    private final Player player;
    private Function<Player, String> header;
    private Function<Player, String> footer;
    private Function<Player, String> name;
    private ToIntFunction<Player> order;
    private boolean placeholders;
    private int refreshTicks = -1;
    private boolean resetOnClose = true;

    public TabBuilder(@NotNull FoliaBoard board, @NotNull Player player) {
        this.board = board;
        this.player = player;
    }

    public @NotNull TabBuilder header(@NotNull String anyFormat) {
        this.header = viewer -> anyFormat;
        return this;
    }

    public @NotNull TabBuilder header(@NotNull List<String> lines) {
        return header(String.join("\n", lines));
    }

    public @NotNull TabBuilder header(@NotNull Function<Player, String> header) {
        this.header = header;
        return this;
    }

    public @NotNull TabBuilder footer(@NotNull String anyFormat) {
        this.footer = viewer -> anyFormat;
        return this;
    }

    public @NotNull TabBuilder footer(@NotNull List<String> lines) {
        return footer(String.join("\n", lines));
    }

    public @NotNull TabBuilder footer(@NotNull Function<Player, String> footer) {
        this.footer = footer;
        return this;
    }

    public @NotNull TabBuilder name(@NotNull String anyFormat) {
        this.name = viewer -> anyFormat;
        return this;
    }

    public @NotNull TabBuilder name(@NotNull Function<Player, String> name) {
        this.name = name;
        return this;
    }

    public @NotNull TabBuilder order(int order) {
        this.order = viewer -> order;
        return this;
    }

    public @NotNull TabBuilder order(@NotNull ToIntFunction<Player> order) {
        this.order = order;
        return this;
    }

    public @NotNull TabBuilder orderByPermission(@NotNull String... permissionsHighestFirst) {
        String[] copy = permissionsHighestFirst.clone();
        this.order = viewer -> {
            for (int index = 0; index < copy.length; index++) {
                if (viewer.hasPermission(copy[index])) {
                    return (copy.length - index) * 100;
                }
            }
            return 0;
        };
        return this;
    }

    public @NotNull TabBuilder placeholders(boolean enabled) {
        this.placeholders = enabled;
        return this;
    }

    public @NotNull TabBuilder refreshEvery(int ticks) {
        this.refreshTicks = Math.max(1, ticks);
        return this;
    }

    public @NotNull TabBuilder resetOnClose(boolean reset) {
        this.resetOnClose = reset;
        return this;
    }

    public @NotNull TabList build() {
        TabImpl tab = new TabImpl(board.plugin(), board.placeholders(), player,
                new TabImpl.Spec(header, footer, name, order, placeholders, refreshTicks, resetOnClose));
        board.trackTab(player, tab);
        tab.start();
        return tab;
    }
}
