package net.foliaboard.internal.packet.reflect;

import net.foliaboard.api.format.NumberFormat;
import net.foliaboard.internal.packet.DisplaySlotType;
import net.foliaboard.internal.packet.PacketAdapter;
import net.foliaboard.internal.packet.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

public final class NmsPacketAdapter implements PacketAdapter {
    private final ComponentConverter components = new ComponentConverter();

    private final Method craftPlayerGetHandle;
    private final Field connectionField;
    private final Method sendMethod;
    private final MethodHandle getHandleMH;
    private final MethodHandle connectionGetterMH;
    private final MethodHandle sendMH;

    private final Object dummyScoreboard;
    private final Object dummyCriteria;
    private final Object renderTypeInteger;
    private final Constructor<?> objectiveCtor;
    private final int objectiveCtorArgs;
    private final Object blankNumberFormat;
    private final Constructor<?> fixedFormatCtor;
    private final Constructor<?> styledFormatCtor;
    private final Method adventureStyleToVanilla;
    private final Constructor<?> setObjectivePacketCtor;
    private final Method objectiveGetName;

    private final Class<?> displaySlotClass;
    private final Constructor<?> setDisplayPacketCtor;

    private final Constructor<?> setScorePacketCtor;
    private final Constructor<?> resetScorePacketCtor;
    private final boolean scoreDisplayOptional;
    private final boolean scoreFormatOptional;
    private final boolean resetObjectiveOptional;

    private final Class<?> playerTeamClass;
    private final Constructor<?> playerTeamCtor;
    private final Method teamSetPrefix;
    private final Method teamSetSuffix;
    private final Method teamSetDisplayName;
    private final Method teamSetColor;
    private final Method teamSetNametagVisibility;
    private final Method teamSetCollision;
    private final Method teamSetFriendlyFire;
    private final Method teamSetSeeInvisibles;
    private final Method teamGetPlayers;
    private final Class<?> chatFormattingClass;
    private final Class<?> teamVisibilityClass;
    private final Class<?> teamCollisionClass;
    private final Method createAddOrModify;
    private final Method createRemove;
    private final Method createPlayerPacket;
    private final Class<?> teamActionClass;

    public NmsPacketAdapter() {
        Class<?> craftPlayer = Reflect.firstClass(
                "org.bukkit.craftbukkit.entity.CraftPlayer");
        craftPlayerGetHandle = Reflect.method(craftPlayer, "getHandle");
        Class<?> serverPlayer = Reflect.clazz("net.minecraft.server.level.ServerPlayer");
        Class<?> gameListener = Reflect.clazz("net.minecraft.server.network.ServerGamePacketListenerImpl");
        connectionField = Reflect.fieldByTypeDeep(serverPlayer, gameListener);
        Class<?> packetClass = Reflect.clazz("net.minecraft.network.protocol.Packet");

        sendMethod = findSend(gameListener, packetClass);

        MethodHandle gh = null, cg = null, snd = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            gh = lookup.unreflect(craftPlayerGetHandle);
            cg = lookup.unreflectGetter(connectionField);
            snd = lookup.unreflect(sendMethod);
        } catch (Throwable ignored) {
            gh = null;
            cg = null;
            snd = null;
        }
        getHandleMH = gh;
        connectionGetterMH = cg;
        sendMH = snd;

        Class<?> scoreboardClass = Reflect.clazz("net.minecraft.world.scores.Scoreboard");
        dummyScoreboard = Reflect.instantiate(Reflect.constructor(scoreboardClass));
        Class<?> criteriaClass = Reflect.clazz("net.minecraft.world.scores.criteria.ObjectiveCriteria");
        dummyCriteria = Reflect.get(Reflect.field(criteriaClass, "DUMMY"), null);
        Class<?> renderTypeClass = Reflect.clazz(
                "net.minecraft.world.scores.criteria.ObjectiveCriteria$RenderType");
        renderTypeInteger = Reflect.enumValue(renderTypeClass, "INTEGER");
        Class<?> objectiveClass = Reflect.clazz("net.minecraft.world.scores.Objective");
        Constructor<?> objCtor;
        int objArgs;
        try {
            objCtor = Reflect.constructorByCount(objectiveClass, 7);
            objArgs = 7;
        } catch (RuntimeException e7) {
            objCtor = Reflect.constructorByCount(objectiveClass, 5);
            objArgs = 5;
        }
        objectiveCtor = objCtor;
        objectiveCtorArgs = objArgs;
        blankNumberFormat = resolveBlankNumberFormat();
        Class<?> nmsChatComponent = Reflect.clazz("net.minecraft.network.chat.Component");
        fixedFormatCtor = resolveFormatCtor("net.minecraft.network.chat.numbers.FixedFormat", nmsChatComponent);
        Class<?> nmsStyleClass = tryClass("net.minecraft.network.chat.Style");
        styledFormatCtor = nmsStyleClass == null ? null
                : resolveFormatCtor("net.minecraft.network.chat.numbers.StyledFormat", nmsStyleClass);
        adventureStyleToVanilla = resolvePaperStyleConverter(nmsStyleClass);
        objectiveGetName = Reflect.method(objectiveClass, "getName");
        Class<?> setObjectivePacket = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundSetObjectivePacket");
        setObjectivePacketCtor = Reflect.constructor(setObjectivePacket, objectiveClass, int.class);

        displaySlotClass = Reflect.clazz("net.minecraft.world.scores.DisplaySlot");
        Class<?> setDisplayPacket = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket");
        setDisplayPacketCtor = Reflect.constructor(setDisplayPacket, displaySlotClass, objectiveClass);

        Class<?> setScorePacket = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundSetScorePacket");
        setScorePacketCtor = Reflect.constructorByCount(setScorePacket, 5);
        Class<?>[] scoreParams = setScorePacketCtor.getParameterTypes();
        scoreDisplayOptional = scoreParams[3] == java.util.Optional.class;
        scoreFormatOptional = scoreParams[4] == java.util.Optional.class;
        Class<?> resetScorePacket = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundResetScorePacket");
        resetScorePacketCtor = Reflect.constructorByCount(resetScorePacket, 2);
        resetObjectiveOptional = resetScorePacketCtor.getParameterTypes()[1] == java.util.Optional.class;

        playerTeamClass = Reflect.clazz("net.minecraft.world.scores.PlayerTeam");
        playerTeamCtor = Reflect.constructor(playerTeamClass, scoreboardClass, String.class);
        chatFormattingClass = Reflect.clazz("net.minecraft.ChatFormatting");
        teamVisibilityClass = Reflect.clazz("net.minecraft.world.scores.Team$Visibility");
        teamCollisionClass = Reflect.clazz("net.minecraft.world.scores.Team$CollisionRule");
        teamSetPrefix = Reflect.optionalMethodDeep(playerTeamClass, "setPlayerPrefix", 1);
        teamSetSuffix = Reflect.optionalMethodDeep(playerTeamClass, "setPlayerSuffix", 1);
        teamSetDisplayName = Reflect.optionalMethodDeep(playerTeamClass, "setDisplayName", 1);
        teamSetColor = Reflect.optionalMethodDeep(playerTeamClass, "setColor", 1);
        teamSetNametagVisibility = Reflect.optionalMethodDeep(playerTeamClass, "setNameTagVisibility", 1);
        teamSetCollision = Reflect.optionalMethodDeep(playerTeamClass, "setCollisionRule", 1);
        teamSetFriendlyFire = Reflect.optionalMethodDeep(playerTeamClass, "setAllowFriendlyFire", 1);
        teamSetSeeInvisibles = Reflect.optionalMethodDeep(playerTeamClass, "setSeeFriendlyInvisibles", 1);
        teamGetPlayers = Reflect.optionalMethodDeep(playerTeamClass, "getPlayers", 0);
        warnMissingTeamMethods();
        Class<?> teamPacket = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
        createAddOrModify = Reflect.method(teamPacket, "createAddOrModifyPacket", playerTeamClass, boolean.class);
        createRemove = Reflect.method(teamPacket, "createRemovePacket", playerTeamClass);
        teamActionClass = Reflect.clazz(
                "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Action");
        createPlayerPacket = Reflect.method(teamPacket, "createPlayerPacket",
                playerTeamClass, String.class, teamActionClass);

        initTabSupport(serverPlayer);
    }

    private boolean tabNameSupported;
    private Constructor<?> playerInfoCtor;
    private Object updateDisplayNameAction;
    private Field playerInfoEntriesField;
    private Class<?> nmsComponentClass;
    private Constructor<?> entryCanonicalCtor;
    private java.lang.reflect.RecordComponent[] entryComponents;
    private int displayNameIndex = -1;

    private void initTabSupport(Class<?> serverPlayer) {
        try {
            Class<?> packet = Reflect.clazz("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Reflect.clazz(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            updateDisplayNameAction = Reflect.enumValue(actionClass, "UPDATE_DISPLAY_NAME");
            playerInfoCtor = packet.getDeclaredConstructor(actionClass, serverPlayer);
            playerInfoCtor.setAccessible(true);
            playerInfoEntriesField = Reflect.fieldByTypeDeep(packet, java.util.List.class);
            nmsComponentClass = Reflect.clazz("net.minecraft.network.chat.Component");

            Class<?> entryClass = Reflect.clazz(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
            entryComponents = entryClass.getRecordComponents();
            displayNameIndex = locateDisplayName(entryComponents);

            Class<?>[] types = new Class<?>[entryComponents.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = entryComponents[i].getType();
            }
            entryCanonicalCtor = entryClass.getDeclaredConstructor(types);
            entryCanonicalCtor.setAccessible(true);
            tabNameSupported = true;
        } catch (Throwable t) {
            tabNameSupported = false;
        }
    }

    private int locateDisplayName(java.lang.reflect.RecordComponent[] components) {
        for (int i = 0; i < components.length; i++) {
            if (components[i].getType() == nmsComponentClass && components[i].getName().equals("displayName")) {
                return i;
            }
        }
        int found = -1;
        int count = 0;
        for (int i = 0; i < components.length; i++) {
            if (components[i].getType() == nmsComponentClass) {
                count++;
                found = i;
            }
        }
        if (count != 1) {
            throw new IllegalStateException("Cannot unambiguously locate the tab display-name component "
                    + "(" + count + " Component-typed fields); per-viewer tab names disabled.");
        }
        return found;
    }

    @Override
    public boolean supportsPerViewerTab() {
        return tabNameSupported;
    }

    @Override
    public boolean tabDisplayName(Player viewer, Player target, Component displayName) {
        if (!tabNameSupported) {
            return false;
        }
        try {
            Object targetHandle = craftPlayerGetHandle.invoke(target);
            Object packet = playerInfoCtor.newInstance(updateDisplayNameAction, targetHandle);
            java.util.List<?> entries = (java.util.List<?>) playerInfoEntriesField.get(packet);
            if (entries == null || entries.isEmpty()) {
                return false;
            }
            Object entry = entries.get(0);
            Object nmsName = displayName == null ? null : components.toVanilla(displayName);

            Object[] args = new Object[entryComponents.length];
            for (int i = 0; i < entryComponents.length; i++) {
                args[i] = (i == displayNameIndex) ? nmsName : entryComponents[i].getAccessor().invoke(entry);
            }
            Object rebuilt = entryCanonicalCtor.newInstance(args);
            playerInfoEntriesField.set(packet, java.util.Collections.singletonList(rebuilt));
            send(viewer, packet);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Constructor<?> resolveFormatCtor(String className, Class<?> argType) {
        try {
            Class<?> c = Class.forName(className);
            Constructor<?> ctor = c.getDeclaredConstructor(argType);
            ctor.setAccessible(true);
            return ctor;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method resolvePaperStyleConverter(Class<?> nmsStyleClass) {
        if (nmsStyleClass == null) {
            return null;
        }
        try {
            Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            for (Method m : paperAdventure.getDeclaredMethods()) {
                if (m.getName().equals("asVanilla") && m.getParameterCount() == 1
                        && m.getReturnType() == nmsStyleClass) {
                    m.setAccessible(true);
                    return m;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object toNmsNumberFormat(NumberFormat format) {
        if (format == null || format instanceof NumberFormat.Default) {
            return null;
        }
        if (format instanceof NumberFormat.Blank) {
            return blankNumberFormat;
        }
        if (format instanceof NumberFormat.Fixed fixed && fixedFormatCtor != null) {
            return Reflect.instantiate(fixedFormatCtor, components.toVanilla(fixed.component()));
        }
        if (format instanceof NumberFormat.Styled styled && styledFormatCtor != null
                && adventureStyleToVanilla != null) {
            try {
                Object nmsStyle = adventureStyleToVanilla.invoke(null, styled.style());
                return Reflect.instantiate(styledFormatCtor, nmsStyle);
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static Object resolveBlankNumberFormat() {
        try {
            Class<?> blank = Class.forName("net.minecraft.network.chat.numbers.BlankFormat");
            Field instance = blank.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            return instance.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findSend(Class<?> listener, Class<?> packetClass) {
        Class<?> c = listener;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("send") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(packetClass)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        throw new IllegalStateException("FoliaBoard: could not find connection#send(Packet)");
    }

    @Override
    public String describe() {
        return "NmsPacketAdapter (reflection, Mojang-mapped, 1.20.6+)";
    }

    private volatile net.foliaboard.internal.metrics.PacketMetrics metrics;

    @Override
    public void attachMetrics(net.foliaboard.internal.metrics.PacketMetrics metrics) {
        this.metrics = metrics;
    }

    private static final boolean DEBUG = Boolean.getBoolean("foliaboard.debug");

    private void send(Player viewer, Object packet) {
        net.foliaboard.internal.metrics.PacketMetrics m = metrics;
        if (m != null) {
            m.sent();
        }
        try {
            if (DEBUG) {
                java.util.logging.Logger.getLogger("FoliaBoard")
                        .info("[send] " + packet.getClass().getSimpleName() + " -> " + viewer.getName());
            }
            Object connection;
            if (sendMH != null) {
                Object handle = getHandleMH.invoke(viewer);
                connection = connectionGetterMH.invoke(handle);
                if (connection == null) {
                    return;
                }
                sendMH.invoke(connection, packet);
            } else {
                Object handle = craftPlayerGetHandle.invoke(viewer);
                connection = connectionField.get(handle);
                if (connection == null) {
                    return;
                }
                sendMethod.invoke(connection, packet);
            }
        } catch (Throwable t) {
            if (!viewer.isOnline()) {
                return;
            }
            throw new IllegalStateException("FoliaBoard: failed to send packet to " + viewer.getName(), t);
        }
    }

    private Object buildObjective(String id, Component title) {
        Object nmsTitle = components.toVanilla(title);
        if (objectiveCtorArgs == 7) {
            return Reflect.instantiate(objectiveCtor,
                    dummyScoreboard, id, dummyCriteria, nmsTitle, renderTypeInteger, false, null);
        }
        return Reflect.instantiate(objectiveCtor,
                dummyScoreboard, id, dummyCriteria, nmsTitle, renderTypeInteger);
    }

    @Override
    public void createObjective(Player viewer, String objectiveId, Component title) {
        send(viewer, Reflect.instantiate(setObjectivePacketCtor, buildObjective(objectiveId, title), 0));
    }

    @Override
    public void updateObjective(Player viewer, String objectiveId, Component title) {
        send(viewer, Reflect.instantiate(setObjectivePacketCtor, buildObjective(objectiveId, title), 2));
    }

    @Override
    public void removeObjective(Player viewer, String objectiveId) {
        send(viewer, Reflect.instantiate(setObjectivePacketCtor,
                buildObjective(objectiveId, Component.empty()), 1));
    }

    @Override
    public void setDisplaySlot(Player viewer, String objectiveId, DisplaySlotType slot) {
        Object nmsSlot = Reflect.enumValue(displaySlotClass, slot.nmsName());
        Object objective = buildObjective(objectiveId, Component.empty());
        send(viewer, Reflect.instantiate(setDisplayPacketCtor, nmsSlot, objective));
    }

    @Override
    public void setScore(Player viewer, String objectiveId, String entry, int value,
                         Component displayName, NumberFormat numberFormat) {
        Object nmsDisplay = displayName == null ? null : components.toVanilla(displayName);
        Object nmsFormat = toNmsNumberFormat(numberFormat);

        Object displayArg = scoreDisplayOptional ? java.util.Optional.ofNullable(nmsDisplay) : nmsDisplay;
        Object formatArg = scoreFormatOptional ? java.util.Optional.ofNullable(nmsFormat) : nmsFormat;
        send(viewer, Reflect.instantiate(setScorePacketCtor, entry, objectiveId, value, displayArg, formatArg));
    }

    @Override
    public void resetScore(Player viewer, String objectiveId, String entry) {
        Object objectiveArg = resetObjectiveOptional ? java.util.Optional.of(objectiveId) : objectiveId;
        send(viewer, Reflect.instantiate(resetScorePacketCtor, entry, objectiveArg));
    }

    private Object buildTeam(TeamData data, Collection<String> entries) {
        Object team = Reflect.instantiate(playerTeamCtor, dummyScoreboard, data.name());
        if (teamSetDisplayName != null) {
            Reflect.invoke(teamSetDisplayName, team, components.toVanilla(data.displayName()));
        }
        if (teamSetPrefix != null) {
            Reflect.invoke(teamSetPrefix, team, components.toVanilla(data.prefix()));
        }
        if (teamSetSuffix != null) {
            Reflect.invoke(teamSetSuffix, team, components.toVanilla(data.suffix()));
        }
        if (teamSetNametagVisibility != null) {
            Reflect.invoke(teamSetNametagVisibility, team,
                    Reflect.enumValue(teamVisibilityClass, data.nametagVisibility().nmsName()));
        }
        if (teamSetCollision != null) {
            Reflect.invoke(teamSetCollision, team,
                    Reflect.enumValue(teamCollisionClass, data.collision().nmsName()));
        }
        if (teamSetFriendlyFire != null) {
            Reflect.invoke(teamSetFriendlyFire, team, data.friendlyFire());
        }
        if (teamSetSeeInvisibles != null) {
            Reflect.invoke(teamSetSeeInvisibles, team, data.seeFriendlyInvisibles());
        }
        NamedTextColor color = data.color();
        if (color != null && teamSetColor != null) {
            Object chatFormatting = chatFormattingFor(color);
            if (chatFormatting != null) {
                try {
                    Reflect.invoke(teamSetColor, team, chatFormatting);
                } catch (RuntimeException ignored) {
                }
            }
        }
        if (entries != null && !entries.isEmpty() && teamGetPlayers != null) {
            Collection<String> players = Reflect.invoke(teamGetPlayers, team);
            players.addAll(entries);
        }
        return team;
    }

    private void warnMissingTeamMethods() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (teamSetPrefix == null) missing.add("setPlayerPrefix");
        if (teamSetSuffix == null) missing.add("setPlayerSuffix");
        if (teamSetDisplayName == null) missing.add("setDisplayName");
        if (teamSetColor == null) missing.add("setColor");
        if (teamSetNametagVisibility == null) missing.add("setNameTagVisibility");
        if (teamSetCollision == null) missing.add("setCollisionRule");
        if (teamSetFriendlyFire == null) missing.add("setAllowFriendlyFire");
        if (teamSetSeeInvisibles == null) missing.add("setSeeFriendlyInvisibles");
        if (teamGetPlayers == null) missing.add("getPlayers");
        if (!missing.isEmpty()) {
            java.util.logging.Logger.getLogger("FoliaBoard").warning(
                    "FoliaBoard: PlayerTeam methods not found on this server (" + String.join(", ", missing)
                    + "); the matching nametag/team styling is disabled. The sidebar is unaffected.");
        }
    }

    private Object chatFormattingFor(NamedTextColor color) {
        try {
            return Reflect.enumValue(chatFormattingClass, color.toString().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public void createTeam(Player viewer, TeamData team, Collection<String> entries) {
        Object nmsTeam = buildTeam(team, entries);
        send(viewer, Reflect.invoke(createAddOrModify, null, nmsTeam, true));
    }

    @Override
    public void updateTeam(Player viewer, TeamData team) {
        Object nmsTeam = buildTeam(team, null);
        send(viewer, Reflect.invoke(createAddOrModify, null, nmsTeam, false));
    }

    @Override
    public void removeTeam(Player viewer, String teamName) {
        Object nmsTeam = Reflect.instantiate(playerTeamCtor, dummyScoreboard, teamName);
        send(viewer, Reflect.invoke(createRemove, null, nmsTeam));
    }

    @Override
    public void teamEntries(Player viewer, String teamName, Collection<String> entries, boolean add) {
        Object nmsTeam = Reflect.instantiate(playerTeamCtor, dummyScoreboard, teamName);
        Object action = Reflect.enumValue(teamActionClass, add ? "ADD" : "REMOVE");
        for (String entry : entries) {
            send(viewer, Reflect.invoke(createPlayerPacket, null, nmsTeam, entry, action));
        }
    }
}
