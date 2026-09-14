package net.foliaboard.api.placeholder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Placeholders {
    private static final Pattern TOKEN = Pattern.compile("%([^%]+)%");

    private final List<PlaceholderResolver> resolvers = new CopyOnWriteArrayList<>();
    private final Map<String, Function<Player, String>> keyed = new ConcurrentHashMap<>();
    private final boolean papiPresent;
    private Method papiSetPlaceholders;

    public Placeholders() {
        boolean present = false;
        try {
            present = Bukkit.getServer() != null
                    && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
            if (present) {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                papiSetPlaceholders = papi.getMethod("setPlaceholders",
                        org.bukkit.OfflinePlayer.class, String.class);
            }
        } catch (Throwable t) {
            present = false;
        }
        this.papiPresent = present;
    }

    public @NotNull Placeholders register(@NotNull PlaceholderResolver resolver) {
        resolvers.add(0, resolver);
        return this;
    }

    public @NotNull Placeholders register(@NotNull String key, @NotNull Function<Player, String> value) {
        keyed.put(key.toLowerCase(Locale.ROOT), value);
        return this;
    }

    public @NotNull Placeholders unregister(@NotNull String key) {
        keyed.remove(key.toLowerCase(Locale.ROOT));
        return this;
    }

    public @NotNull String apply(@NotNull Player player, @NotNull String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        boolean anyUnresolved = false;
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = resolveLocal(player, key);
            if (replacement == null) {
                anyUnresolved = true;
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(out);
        String result = out.toString();

        if (anyUnresolved && papiPresent) {
            result = applyPapi(player, result);
        }
        return result;
    }

    public @NotNull Component component(@NotNull Player player, @NotNull String miniMessageWithPlaceholders) {
        return MiniMessage.miniMessage().deserialize(resolveEscaped(player, miniMessageWithPlaceholders));
    }

    public @NotNull String resolveForMiniMessage(@NotNull Player player, @NotNull String text) {
        return resolveEscaped(player, text);
    }

    private String resolveEscaped(Player player, String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group();
            String key = matcher.group(1);
            String value = resolveLocal(player, key);
            if (value == null && papiPresent) {
                String papi = applyPapi(player, token);
                value = papi.equals(token) ? null : papi;
            }
            String replacement = value == null ? token : escape(value);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String escape(String value) {
        if (value.indexOf('<') < 0 && value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '<' || character == '\\') {
                escaped.append('\\');
            }
            escaped.append(character);
        }
        return escaped.toString();
    }

    private String resolveLocal(Player player, String key) {
        Function<Player, String> direct = keyed.isEmpty() ? null : keyed.get(key.toLowerCase(Locale.ROOT));
        if (direct != null) {
            String value = direct.apply(player);
            if (value != null) {
                return value;
            }
        }
        for (PlaceholderResolver resolver : resolvers) {
            String v = resolver.resolve(player, key);
            if (v != null) {
                return v;
            }
        }
        return builtin(player, key);
    }

    private static String builtin(Player player, String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "player", "player_name", "name" -> player.getName();
            case "displayname" -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(player.displayName());
            case "world" -> player.getWorld().getName();
            case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "ping" -> String.valueOf(player.getPing());
            case "health" -> String.valueOf((int) Math.round(player.getHealth()));
            case "x" -> String.valueOf(player.getLocation().getBlockX());
            case "y" -> String.valueOf(player.getLocation().getBlockY());
            case "z" -> String.valueOf(player.getLocation().getBlockZ());
            default -> null;
        };
    }

    private String applyPapi(Player player, String text) {
        try {
            return (String) papiSetPlaceholders.invoke(null, player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    public boolean placeholderApiPresent() {
        return papiPresent;
    }
}
