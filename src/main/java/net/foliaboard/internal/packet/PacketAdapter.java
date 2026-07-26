package net.foliaboard.internal.packet;

import net.foliaboard.api.format.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface PacketAdapter {
    String describe();

    default void attachMetrics(net.foliaboard.internal.metrics.PacketMetrics metrics) {
    }

    default boolean supportsPerViewerTab() {
        return false;
    }

    default boolean tabDisplayName(Player viewer, Player target, @Nullable Component displayName) {
        return false;
    }

    void createObjective(Player viewer, String objectiveId, Component title);

    void updateObjective(Player viewer, String objectiveId, Component title);

    void removeObjective(Player viewer, String objectiveId);

    void setDisplaySlot(Player viewer, String objectiveId, DisplaySlotType slot);

    void setScore(Player viewer, String objectiveId, String entry, int value,
                  @Nullable Component displayName, @Nullable NumberFormat numberFormat);

    void resetScore(Player viewer, String objectiveId, String entry);

    void createTeam(Player viewer, TeamData team, Collection<String> entries);

    void updateTeam(Player viewer, TeamData team);

    void removeTeam(Player viewer, String teamName);

    void teamEntries(Player viewer, String teamName, Collection<String> entries, boolean add);
}
