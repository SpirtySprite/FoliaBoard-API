package net.foliaboard.internal.packet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeamData {
    public enum Visibility {
        ALWAYS("ALWAYS"),
        NEVER("NEVER"),
        HIDE_FOR_OTHER_TEAMS("HIDE_FOR_OTHER_TEAMS"),
        HIDE_FOR_OWN_TEAM("HIDE_FOR_OWN_TEAM");

        private final String nmsName;

        Visibility(String nmsName) {
            this.nmsName = nmsName;
        }

        public String nmsName() {
            return nmsName;
        }
    }

    public enum Collision {
        ALWAYS("ALWAYS"),
        NEVER("NEVER"),
        PUSH_OTHER_TEAMS("PUSH_OTHER_TEAMS"),
        PUSH_OWN_TEAM("PUSH_OWN_TEAM");

        private final String nmsName;

        Collision(String nmsName) {
            this.nmsName = nmsName;
        }

        public String nmsName() {
            return nmsName;
        }
    }

    private final String name;
    private Component displayName = Component.empty();
    private Component prefix = Component.empty();
    private Component suffix = Component.empty();
    private @Nullable NamedTextColor color = null;
    private Visibility nametagVisibility = Visibility.ALWAYS;
    private Collision collision = Collision.ALWAYS;
    private boolean friendlyFire = true;
    private boolean seeFriendlyInvisibles = false;

    public TeamData(@NotNull String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public TeamData copy() {
        TeamData c = new TeamData(name);
        c.displayName = displayName;
        c.prefix = prefix;
        c.suffix = suffix;
        c.color = color;
        c.nametagVisibility = nametagVisibility;
        c.collision = collision;
        c.friendlyFire = friendlyFire;
        c.seeFriendlyInvisibles = seeFriendlyInvisibles;
        return c;
    }

    public Component displayName() {
        return displayName;
    }

    public TeamData displayName(Component displayName) {
        this.displayName = displayName == null ? Component.empty() : displayName;
        return this;
    }

    public Component prefix() {
        return prefix;
    }

    public TeamData prefix(Component prefix) {
        this.prefix = prefix == null ? Component.empty() : prefix;
        return this;
    }

    public Component suffix() {
        return suffix;
    }

    public TeamData suffix(Component suffix) {
        this.suffix = suffix == null ? Component.empty() : suffix;
        return this;
    }

    public @Nullable NamedTextColor color() {
        return color;
    }

    public TeamData color(@Nullable NamedTextColor color) {
        this.color = color;
        return this;
    }

    public Visibility nametagVisibility() {
        return nametagVisibility;
    }

    public TeamData nametagVisibility(Visibility visibility) {
        this.nametagVisibility = visibility;
        return this;
    }

    public Collision collision() {
        return collision;
    }

    public TeamData collision(Collision collision) {
        this.collision = collision;
        return this;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public TeamData friendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
        return this;
    }

    public boolean seeFriendlyInvisibles() {
        return seeFriendlyInvisibles;
    }

    public TeamData seeFriendlyInvisibles(boolean seeFriendlyInvisibles) {
        this.seeFriendlyInvisibles = seeFriendlyInvisibles;
        return this;
    }
}
