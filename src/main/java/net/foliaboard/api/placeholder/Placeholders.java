package net.foliaboard.api.placeholder;

import net.foliaboard.api.text.Legacy;
import net.foliaboard.api.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Placeholders {
    private static final Pattern TOKEN = Pattern.compile("%([^%\\s]+)%");
    private static final long PAPI_RETRY_MILLIS = 5_000L;

    private record Keyed(Function<Player, String> value, long ttlMillis) {
    }

    private record CacheKey(UUID player, String key) {
    }

    private record Cached(String value, long expiresAt) {
    }

    private final List<PlaceholderResolver> resolvers = new CopyOnWriteArrayList<>();
    private final Map<String, Keyed> keyed = new ConcurrentHashMap<>();
    private final Map<CacheKey, Cached> cache = new ConcurrentHashMap<>();
    private volatile Method papiSetPlaceholders;
    private volatile long papiCheckedAt = Long.MIN_VALUE;
    private volatile long papiTtlMillis;
    private volatile boolean convertLegacy = true;

    public Placeholders() {
        detectPapi(System.currentTimeMillis());
    }

    public @NotNull Placeholders register(@NotNull PlaceholderResolver resolver) {
        resolvers.add(0, resolver);
        return this;
    }

    public @NotNull Placeholders unregister(@NotNull PlaceholderResolver resolver) {
        resolvers.remove(resolver);
        return this;
    }

    public @NotNull Placeholders register(@NotNull String key, @NotNull Function<Player, String> value) {
        return register(key, value, Duration.ZERO);
    }

    public @NotNull Placeholders register(@NotNull String key, @NotNull Function<Player, String> value,
                                          @NotNull Duration cacheFor) {
        String normalized = key.toLowerCase(Locale.ROOT);
        keyed.put(normalized, new Keyed(value, Math.max(0L, cacheFor.toMillis())));
        invalidate(normalized);
        return this;
    }

    public @NotNull Placeholders unregister(@NotNull String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        keyed.remove(normalized);
        invalidate(normalized);
        return this;
    }

    public @NotNull Placeholders cachePlaceholderApi(@NotNull Duration cacheFor) {
        this.papiTtlMillis = Math.max(0L, cacheFor.toMillis());
        return this;
    }

    public @NotNull Placeholders convertLegacyColors(boolean enabled) {
        this.convertLegacy = enabled;
        return this;
    }

    public void forget(@NotNull UUID player) {
        cache.keySet().removeIf(key -> key.player().equals(player));
    }

    public void invalidate(@NotNull String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        cache.keySet().removeIf(entry -> entry.key().equals(normalized));
    }

    public void clearCache() {
        cache.clear();
    }

    public int cachedValues() {
        return cache.size();
    }

    public @NotNull String apply(@NotNull Player player, @NotNull String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder(text.length() + 16);
        while (matcher.find()) {
            String value = resolve(player, matcher.group(1), matcher.group());
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public @NotNull Component component(@NotNull Player player, @NotNull String miniMessageWithPlaceholders) {
        return MiniMessage.miniMessage().deserialize(resolveForMiniMessage(player, miniMessageWithPlaceholders));
    }

    public @NotNull String resolveForMiniMessage(@NotNull Player player, @NotNull String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder(text.length() + 16);
        while (matcher.find()) {
            String value = resolve(player, matcher.group(1), matcher.group());
            String replacement = value == null ? matcher.group() : render(value);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public @NotNull String value(@NotNull Player player, @NotNull String key) {
        String value = resolve(player, key, "%" + key + "%");
        return value == null ? "" : value;
    }

    private String render(String value) {
        String escaped = Text.escape(value);
        return convertLegacy ? Legacy.toMini(escaped) : Legacy.strip(escaped);
    }

    private String resolve(Player player, String key, String token) {
        String normalized = key.toLowerCase(Locale.ROOT);
        Keyed direct = keyed.isEmpty() ? null : keyed.get(normalized);
        if (direct != null) {
            String value = cachedOrCompute(player, normalized, direct.ttlMillis(), () -> direct.value().apply(player));
            if (value != null) {
                return value;
            }
        }
        for (PlaceholderResolver resolver : resolvers) {
            String value = resolver.resolve(player, key);
            if (value != null) {
                return value;
            }
        }
        String builtin = builtin(player, normalized);
        if (builtin != null) {
            return builtin;
        }
        Method papi = papi();
        if (papi == null) {
            return null;
        }
        String resolved = cachedOrCompute(player, "papi:" + normalized, papiTtlMillis, () -> applyPapi(papi, player, token));
        return resolved == null || resolved.equals(token) ? null : resolved;
    }

    private String cachedOrCompute(Player player, String key, long ttlMillis, java.util.function.Supplier<String> compute) {
        if (ttlMillis <= 0L) {
            return compute.get();
        }
        long now = System.currentTimeMillis();
        CacheKey cacheKey = new CacheKey(player.getUniqueId(), key);
        Cached hit = cache.get(cacheKey);
        if (hit != null && hit.expiresAt() > now) {
            return hit.value();
        }
        String value = compute.get();
        if (value != null) {
            cache.put(cacheKey, new Cached(value, now + ttlMillis));
        }
        return value;
    }

    private static String builtin(Player player, String key) {
        return switch (key) {
            case "player", "player_name", "name" -> player.getName();
            case "displayname" -> Text.plain(player.displayName());
            case "world" -> player.getWorld().getName();
            case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "ping" -> String.valueOf(player.getPing());
            case "health" -> String.valueOf((int) Math.round(player.getHealth()));
            case "level" -> String.valueOf(player.getLevel());
            case "gamemode" -> player.getGameMode().name().toLowerCase(Locale.ROOT);
            case "x" -> String.valueOf(player.getLocation().getBlockX());
            case "y" -> String.valueOf(player.getLocation().getBlockY());
            case "z" -> String.valueOf(player.getLocation().getBlockZ());
            default -> null;
        };
    }

    private Method papi() {
        Method method = papiSetPlaceholders;
        if (method != null) {
            return method;
        }
        long now = System.currentTimeMillis();
        if (now - papiCheckedAt < PAPI_RETRY_MILLIS) {
            return null;
        }
        detectPapi(now);
        return papiSetPlaceholders;
    }

    private void detectPapi(long now) {
        papiCheckedAt = now;
        try {
            if (Bukkit.getServer() == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                return;
            }
            Class<?> type = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            papiSetPlaceholders = type.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
        } catch (Throwable unavailable) {
            papiSetPlaceholders = null;
        }
    }

    private static String applyPapi(Method method, Player player, String text) {
        try {
            return (String) method.invoke(null, player, text);
        } catch (Throwable failure) {
            return text;
        }
    }

    public boolean placeholderApiPresent() {
        return papi() != null;
    }
}
