package net.foliaboard.internal.version;

import org.bukkit.Bukkit;

public final class ServerVersion {
    private final int major;
    private final int minor;
    private final int patch;

    private ServerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static ServerVersion current() {
        String raw;
        try {
            raw = Bukkit.getMinecraftVersion();
        } catch (Throwable t) {
            raw = Bukkit.getBukkitVersion();
        }
        return parse(raw);
    }

    static ServerVersion parse(String raw) {
        String cleaned = raw.split("-")[0].trim();
        String[] parts = cleaned.split("\\.");
        int major = safe(parts, 0);
        int minor = safe(parts, 1);
        int patch = safe(parts, 2);
        return new ServerVersion(major, minor, patch);
    }

    private static int safe(String[] parts, int i) {
        if (i >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isCalendarScheme() {
        return major != 1;
    }

    public boolean isAtLeast(int minor, int patch) {
        if (isCalendarScheme()) {
            return true;
        }
        if (this.minor != minor) {
            return this.minor > minor;
        }
        return this.patch >= patch;
    }

    public boolean supportsScoreDisplayName() {
        return isAtLeast(20, 3);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
