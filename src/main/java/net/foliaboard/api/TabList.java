package net.foliaboard.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface TabList {
    @NotNull Player player();

    @NotNull TabList refresh();

    int sentUpdates();

    void close();

    boolean closed();
}
