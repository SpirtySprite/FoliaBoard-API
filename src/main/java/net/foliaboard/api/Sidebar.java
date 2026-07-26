package net.foliaboard.api;

import net.foliaboard.api.format.NumberFormat;
import net.foliaboard.api.hook.LineProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Sidebar {
    @NotNull Player player();

    @NotNull Component title();

    @NotNull List<Component> lines();

    int lineCount();

    @NotNull Sidebar title(@NotNull ComponentLike title);

    @NotNull Sidebar line(int index, @NotNull ComponentLike text);

    @NotNull Sidebar line(int index, @NotNull ComponentLike text, @NotNull NumberFormat numberFormat);

    @NotNull Sidebar lines(@NotNull List<? extends ComponentLike> lines);

    @NotNull Sidebar removeLine(int index);

    @NotNull Sidebar clearLines();

    @NotNull Sidebar lineProcessors(@NotNull List<LineProcessor> processors);

    @NotNull Sidebar visible(boolean visible);

    boolean visible();

    void close();

    boolean closed();
}
