package net.foliaboard.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Builds the objective and team names FoliaBoard sends to clients.
 *
 * <p>The client keeps one namespace of objectives and teams per player. Two plugins that each
 * shade FoliaBoard would otherwise both create {@code fb0} and overwrite each other, so every
 * name embeds a short hash of the owning plugin's name. Names stay within the classic 16
 * character limit.
 */
public final class Ids {
    public static final int MAX_LENGTH = 16;

    private Ids() {
    }

    public static @NotNull String namespace(@NotNull String pluginName) {
        int h = pluginName.toLowerCase(Locale.ROOT).hashCode();
        int folded = (h ^ (h >>> 16)) & 0xFFFF;
        return String.format(Locale.ROOT, "%04x", folded);
    }

    public static @NotNull String sidebarObjective(@NotNull String namespace, int counter) {
        return checked("fb" + namespace + Integer.toHexString(counter));
    }

    public static @NotNull String belowNameObjective(@NotNull String namespace) {
        return checked("fb" + namespace + "_bn");
    }

    public static @NotNull String tabListObjective(@NotNull String namespace) {
        return checked("fb" + namespace + "_tab");
    }

    public static @NotNull String team(@NotNull String namespace, @Nullable Integer sortWeight, int counter) {
        String unique = namespace + Integer.toHexString(counter);
        String name = sortWeight != null
                ? String.format(Locale.ROOT, "%04d", Math.max(0, Math.min(9999, sortWeight))) + unique
                : "fbn" + unique;
        return checked(name);
    }

    private static String checked(String name) {
        if (name.length() > MAX_LENGTH) {
            throw new IllegalStateException("FoliaBoard: generated name exceeds " + MAX_LENGTH + " chars: " + name);
        }
        return name;
    }
}
