# FoliaBoard

A **Folia-native, packet-level scoreboard API** for Paper & Folia. It removes the single hardest part
of scoreboards on Folia — knowing *which thread* may touch a player's board and not racing when they
move between regions — and gives you a fluent, MiniMessage-first API where **every call is safe from
any thread**.

```java
FoliaBoard board = FoliaBoard.create(this);

board.createBoard(player)
     .placeholders(true)
     .title("<gradient:#00c6ff:#0072ff><bold>MY SERVER</bold></gradient>")
     .blankLine()
     .lines("<gray>Player: <white>%player%",
            "<gray>Online: <green>%online%",
            "<gray>Ping: <aqua>%ping%ms")
     .blankLine()
     .line("<yellow>play.myserver.net")
     .build();
```

- **Zero threading work.** Call from the main thread, an async task, a region thread — anywhere.
- **Zero third-party deps.** Packets are built and sent directly. No ProtocolLib, no MegaVex.
- **MiniMessage everywhere.** Any `String` argument is parsed as MiniMessage (gradients, hover, click…).
- **Runs on Paper too.** The same jar works on plain Paper (everything just runs on the main thread).
- **Validated live on Folia 1.21.11.**

---

## Table of contents

1. [Why it exists](#why-it-exists)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Quick start](#quick-start)
5. [Sidebars](#sidebars)
6. [Layout profiles](#layout-profiles)
7. [Nametags](#nametags)
8. [Below-name & tab-list numbers](#below-name--tab-list-numbers)
9. [Tab-list header & footer](#tab-list-header--footer)
10. [Number formats](#number-formats)
11. [Animations](#animations)
12. [Placeholders](#placeholders)
13. [MiniMessage & text](#minimessage--text)
14. [Events & hooks](#events--hooks)
15. [Async utilities](#async-utilities)
16. [Threading model](#threading-model)
17. [Performance](#performance)
18. [Lifecycle, cleanup & `/reload`](#lifecycle-cleanup--reload)
19. [API reference](#api-reference)
20. [Version support & the honest caveat](#version-support--the-honest-caveat)
21. [Building from source](#building-from-source)

---

## Why it exists

Packet scoreboard libraries are excellent but warn that their board/team objects are **not
thread-safe**. On Folia that warning is the whole game: there is no main thread, players tick on
**region threads**, they **migrate** between regions, and join/quit fires on region threads.

FoliaBoard's core idea: confine every mutation of a player's board to that player's
[`EntityScheduler`](https://docs.papermc.io/folia/reference/region-logic) — Folia's scheduler that
follows the entity across regions and runs its tasks strictly sequentially. That yields thread-safety
with **zero locks**, and it's all hidden. You describe *what* to show; FoliaBoard handles *where* and
*when* it's safe to send the packets.

---

## Requirements

- **Paper or Folia 1.20.6+** (modern per-score display components).
- **Java 21**.

---

## Installation

Two ways to use it (see [`INSTALLATION.md`](INSTALLATION.md) for the full guide):

- **Shade** `foliaboard-core` into your plugin (self-contained; ships **no `plugin.yml`** so it can't
  clobber yours) — shown below.
- **Install the standalone `FoliaBoard` plugin** (the Modrinth download) and `softdepend: [FoliaBoard]`
  — one shared jar for all your plugins; then `ScoreboardAPI.get()...`.

**1. Depend on the core library:**

```xml
<repositories>
  <repository><id>papermc</id><url>https://repo.papermc.io/repository/maven-public/</url></repository>
</repositories>

<dependency>
  <groupId>net.foliaboard</groupId>
  <artifactId>foliaboard-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

**2. Shade + relocate it** (so two plugins bundling it can't collide):

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.6.0</version>
  <executions><execution><phase>package</phase><goals><goal>shade</goal></goals>
    <configuration><relocations><relocation>
      <pattern>net.foliaboard</pattern>
      <shadedPattern>com.yourplugin.libs.foliaboard</shadedPattern>
    </relocation></relocations></configuration>
  </execution></executions>
</plugin>
```

**3. Mark your plugin Folia-ready** — required or it won't load on Folia:

```yaml
name: YourPlugin
main: com.yourplugin.YourPlugin
version: 1.0.0
api-version: '1.20'
folia-supported: true
softdepend: [PlaceholderAPI]   # optional; enables the placeholder bridge
```

---

## Quick start

Two styles — pick one.

**A. Instance (recommended):**
```java
public final class YourPlugin extends JavaPlugin {
    private FoliaBoard board;

    @Override public void onEnable()  { board = FoliaBoard.create(this); }
    @Override public void onDisable() { if (board != null) board.close(); }
}
```

**B. Static handle** (if you'd rather not pass the instance around):
```java
@Override public void onEnable()  { ScoreboardAPI.init(this); }
@Override public void onDisable() { ScoreboardAPI.shutdown(); }

// anywhere:
ScoreboardAPI.createBoard(player).title("<aqua>Hi").line("<gray>Welcome!").build();
```

All examples below use a `board` (a `FoliaBoard`); with the static handle just write `ScoreboardAPI`
or `ScoreboardAPI.get()`.

Add `import static net.foliaboard.api.text.Text.mini;` (or use `Text.mini(...)`) when you want a
`Component` from a MiniMessage string.

---

## Sidebars

The right-hand board. There are four ways to drive one — from most convenient to most manual.

### 1. Fluent builder

```java
board.createBoard(player)
     .placeholders(true)              // resolve %built-ins% + PlaceholderAPI on strings
     .refreshEvery(4)                 // ticks between auto-refreshes of dynamic content
     .title("<gradient:#00c6ff:#0072ff><bold>MY SERVER</bold></gradient>")
     .blankLine()
     .line("<gray>Player: <white>%player%")
     .lines("<gray>Kills: <red>0", "<gray>Deaths: <red>0")   // several at once
     .blankLine()
     .line("<yellow>play.myserver.net")
     .build();                        // returns the live Sidebar
```

If any content is a placeholder string, an `Animation`, or a supplier, the board is **dynamic** and
FoliaBoard auto-refreshes it on the player's own thread — you never write a scheduler. Everything
else is painted once.

Titles and lines accept `String` (MiniMessage), `Component`, or `Animation<Component>`. Lines may also
carry a [number format](#number-formats).

### 2. Manual control

Full control, still safe from any thread:

```java
Sidebar sb = board.sidebar(player);         // get or create this player's sidebar
sb.title(mini("<gold>Kit Selector"));
sb.line(0, mini("<gray>Coins: <yellow>1500"));
sb.line(1, mini("<gray>Kills"), NumberFormat.fixed(mini("<red>12")));  // per-line number
sb.visible(false);                          // hide without discarding
sb.visible(true);
sb.clearLines();
sb.close();                                 // remove entirely (auto on quit)

// read-back:
Component title = sb.title();
List<Component> lines = sb.lines();
int count = sb.lineCount();
```

Only genuinely-changed lines produce packets, so frequent updates never flicker.

### 3. Global provider — one description for everyone

```java
board.setGlobalSidebar(SidebarProvider.of(
    p -> mini("<aqua>MY SERVER"),
    p -> List.of(mini("<gray>Online: <green>" + Bukkit.getOnlinePlayers().size())),
    10));   // refresh every 10 ticks
```

Or implement the interface for `visible(player)` control. FoliaBoard attaches a sidebar to every
player, refreshes it per player on the right thread, and cleans up on quit.

### 4. Global layout — same shape, per-player content

```java
board.setGlobalSidebar(Layout.named("main", b -> b
    .placeholders(true)
    .title("<aqua>MY SERVER")
    .line("<gray>Rank: <gold>%rank%")));
```

See [Layout profiles](#layout-profiles) for switching between several.

> **One driver per board.** A global provider/layout yields automatically to any explicit
> `createBoard(...).build()` or `applyLayout(...)` for that player, so they never fight.

---

## Layout profiles

A **layout** is a reusable, named board template you can apply to any player and switch between
instantly (lobby ↔ minigame, per-world boards, …). It's just a recorded recipe of builder calls.

```java
Layout lobby = Layout.named("lobby", b -> b
    .placeholders(true)
    .title(Animations.cycle(Duration.ofMillis(400), mini("<aqua>LOBBY"), mini("<white>LOBBY")))
    .blankLine()
    .line("<gray>Rank: <gold>%rank%")
    .line("<gray>Coins: <yellow>%coins%"));

Layout minigame = Layout.named("minigame", b -> b
    .title("<red><bold>SKYWARS</bold>")
    .line("<gray>Kills", NumberFormat.fixed(mini("<red>0"))));

board.registerLayout(lobby).registerLayout(minigame);

board.applyLayout(player, "lobby");          // switch instantly, any time
board.setWorldLayout("minigame_world", "minigame");  // auto-applied on join & world change
board.unregisterLayout("minigame");          // remove a layout
```

- Applying a layout **replaces** the previous board cleanly (no stale leftover lines).
- Leaving a world-layout world for one with no layout **clears** the board.
- Fires a cancellable [`LayoutApplyEvent`](#events--hooks) so other plugins can override per rank/region.

---

## Nametags

Control the text around a player's name (above their head **and** in the tab list): prefix, suffix,
name colour, visibility, collision, and tab-list sort — sent as a scoreboard team, so it doesn't fight
Bukkit teams. Updates use team-*modify*, so they never flicker.

```java
board.createNametag(player)
     .prefix("<gold>[VIP] ")
     .suffix(" <gray>★")
     .color(NamedTextColor.YELLOW)
     .tabSort(10)                       // lower sorts higher in the tab list (0–9999)
     .nametagVisibility(Nametag.Visibility.ALWAYS)
     .collision(Nametag.Collision.NEVER)
     .apply();
```

### Per-viewer nametags

Show a target's name differently to different viewers — classic ally/enemy colouring:

```java
board.createNametag(player)
     .prefix("<gold>[VIP] ")
     .perViewer((viewer, target, style) -> {
         if (areAllies(viewer, target))  style.color(NamedTextColor.GREEN).prefix("<green>✦ ");
         else                            style.color(NamedTextColor.RED).prefix("<red>☠ ");
     })
     .apply();
```

The resolver runs per viewer on that viewer's thread; it starts from the global defaults. Call
`.apply()` again whenever relationships change (e.g. on a timer, or on a team-join event).

---

## Below-name & tab-list numbers

Shared objectives that show a number below every player's name, or beside their tab-list entry.

```java
board.belowName().title(mini("<red>❤")).score(player, 20);   // hearts below the name
board.tabList().score(player, player.getPing());             // ping in the tab list

board.belowName().remove(player.getName());                  // remove one entry
board.belowName().hide();                                    // hide for everyone…
board.belowName().show();                                    // …and bring it back
```

### Per-viewer numbers

```java
board.tabList().scoreFor(viewer, target.getName(), value);   // only `viewer` sees this value
board.tabList().removeFor(viewer, target.getName());         // revert to the shared value
```

Quit players are cleaned up automatically (no leaks, no phantom scores).

---

## Tab-list header & footer

```java
board.tabHeaderFooter(player,
    "<gradient:#00c6ff:#fff><bold>MY SERVER</bold></gradient>",
    "<gray>Online: <green>" + Bukkit.getOnlinePlayers().size());

board.clearTabHeaderFooter(player);
```

Accepts `String` (MiniMessage) or `Component`. Sent on the player's region thread.

### Tab-list entry styling & sorting

Style how a player appears **in the tab list**, independently of their above-head nametag, and sort
the list — **with no scoreboard team**, so it doesn't conflict with other team-based plugins.

```java
board.tabName(player, "<aqua>★ <white>" + player.getName());  // tab prefix ≠ above-head prefix
board.tabOrder(player, staff ? 100 : 0);                      // higher sorts higher (Paper 1.21.2+)
board.resetTabName(player);                                    // back to the vanilla name

if (!board.tabOrderSupported()) { /* pre-1.21.2: use nametag tabSort instead */ }
```

- **Tab vs. above-head are now separate.** The above-head prefix comes from a [nametag](#nametags)
  (a team); the tab prefix comes from `tabName(...)` (no team). Use either or both.
- **Flicker-free, dynamic sorting.** `tabOrder(...)` changes instantly with no team-name trick.
  (On 1.21.1 and older, fall back to nametag `tabSort`.)
- **Team-conflict friendly.** Because tab styling needs no team, a server that already runs a
  team-based nametag/prefix plugin can use FoliaBoard purely for the tab list (and sidebars) without
  fighting over teams. Above-head prefixes still require a team — that's a vanilla limitation — so
  simply don't create FoliaBoard nametags if another plugin owns the above-head text.

`tabName`/`tabOrder` are per-target (shown the same to everyone), built on stable Paper API.

### Per-viewer tab names

To show a target a *different* tab name to *different* viewers, use the packet-level API:

```java
if (board.perViewerTabSupported()) {                 // 1.20.6+ with the player-info packet
    board.tabNameFor(viewer, target, "<red>ENEMY " + target.getName());
    board.resetTabNameFor(viewer, target);           // back to default
}
```

This is a **manual** send (no automatic lifecycle): re-apply it when you need it, e.g. on the
viewer's join or after the server resends player info. It's built on `ClientboundPlayerInfoUpdatePacket`
and **fails safe** — if the server build doesn't support it, `perViewerTabSupported()` returns false
and the calls no-op rather than erroring.

---

## Number formats

Control the red score number the client draws on the right of each entry (1.20.3+). Sidebars hide it
by default; override per line or on shared objectives.

```java
NumberFormat.blank();                              // hide it (sidebar default)
NumberFormat.fixed(mini("<red>✖"));                // replace it with any component
NumberFormat.styled(Style.style(NamedTextColor.GOLD));  // keep the number, restyle it
NumberFormat.defaultFormat();                      // the vanilla red number

board.sidebar(player).line(0, mini("<gray>Kills"), NumberFormat.fixed(mini("<red>12")));
```

---

## Animations

Self-timed — no ticking or registration. Call `current()` and return it; the frame is derived from the
clock.

```java
Animation<Component> title = Animations.cycle(Duration.ofMillis(400),
    mini("<aqua>HUB"), mini("<white>HUB"));

Animation<Component> marquee = Animations.scrollText(Duration.ofMillis(150),
    "welcome to the server!", 24, TextColor.color(0x8AB4F8));

Animation<Component> pulse = Animations.pulseColor(Duration.ofSeconds(2),
    "EVENT LIVE", TextColor.color(0xff0000), TextColor.color(0xffff00));

Animation<Component> typed = Animations.typewriter(Duration.ofMillis(80), Component.text("Loading…"));

// use directly in a builder / layout:
board.createBoard(player).title(title).line(marquee).build();
```

`cycle`, `scrollText`, `pulseColor`, and `typewriter` are code-point safe (won't split emoji).
Animations are global wall-clock phase (every player sees the same frame at the same instant).

---

## Placeholders

A fast engine that replaces `%tokens%` using, in order: your resolvers → built-ins → PlaceholderAPI
(if installed; bridged reflectively, no hard dependency).

```java
board.placeholders().register("rank",  p -> p.isOp() ? "Admin" : "Member");
board.placeholders().register("coins", p -> economy.balance(p));

String  text = board.placeholders().apply(player, "Rank: %rank%");           // -> "Rank: Admin"
Component c  = board.placeholders().component(player, "<gray>Rank: <gold>%rank%");  // MiniMessage + %papi%
```

**Built-ins:** `%player%` / `%player_name%` / `%name%`, `%displayname%`, `%world%`, `%online%`,
`%max_players%`, `%ping%`, `%health%`, `%x%` / `%y%` / `%z%`.

**Injection-safe:** in `component(...)`, placeholder *values* are escaped before parsing, so a value
like a display name containing `<red>` or a PAPI value with `<click:...>` renders literally and can't
inject formatting or click events into your board.

`%built-ins%` such as `%ping%` are cheapest read on the player's own thread — the builder/provider
already do that for you.

---

## MiniMessage & text

`Text` is the one-stop helper.

```java
Component c   = Text.mini("<rainbow>hello</rainbow>");
Component tag = Text.mini("<hover:show_text:'<green>Click!'><click:run_command:/spawn>Spawn</click>");
String    mm  = Text.toMini(someComponent);   // round-trip back to a string
```

Every `String` argument across the API goes through MiniMessage, so you rarely need `Text` directly.

---

## Events & hooks

**Line processor** — rewrite every line/title of every board just before it's sent:

```java
board.addLineProcessor((viewer, index, line) ->
    index == LineProcessor.TITLE ? line
        : line.decoration(TextDecoration.ITALIC, false));   // e.g. kill stray italics
```

**Bukkit events:**

```java
@EventHandler
public void onCreate(SidebarCreateEvent e) {
    getLogger().info("Board created for " + e.getPlayer().getName());
}

@EventHandler
public void onLayout(LayoutApplyEvent e) {          // cancellable + swappable
    if (e.getPlayer().hasPermission("vip")) e.setLayout(board.layout("vip_lobby"));
}
```

Both fire on the player's region thread (synchronous Folia-safe events).

---

## Async utilities

Folia-safe helpers for *your* surrounding work (FoliaBoard's own calls are already thread-safe):

```java
AsyncUtil.async(plugin, () -> {                       // off any game thread
    int coins = db.loadCoins(uuid);
    AsyncUtil.onPlayer(plugin, player, () ->           // hop to the player's region thread
        board.createBoard(player).line("<gold>Coins: " + coins).build());
});

AsyncUtil.asyncLater(plugin, task, Duration.ofSeconds(5));
AsyncUtil.global(plugin, () -> { /* global game state */ });
boolean folia = AsyncUtil.isFolia();
```

---

## Threading model

- **Public API is callable from any thread.** Mutations to a player's board are queued and applied on
  that player's region thread, in order, so nothing races.
- **On Paper** (non-Folia) everything runs on the main thread — the same code, no branches.
- **You never schedule anything** for scoreboard work. For your own logic, use `AsyncUtil`.

Internally: each `Sidebar` keeps *desired* state (written under a lock from any thread) and *sent*
state (touched only on the region thread inside a debounced flush that diffs and emits minimal
packets).

---

## Performance

FoliaBoard is built to stay cheap even with many players and fast, animated boards:

- **Minimal packets.** Sidebars diff desired-vs-sent state and send only changed lines. Below-name/tab
  score updates skip the broadcast entirely when the value is unchanged.
- **No re-parsing.** Dynamic placeholder lines cache their parsed MiniMessage and only re-parse when
  the resolved string actually changes.
- **Cheap conversions.** The (very common) empty component — blank spacer lines, empty titles — is
  converted to its vanilla form once and reused. Score-holder names are interned.
- **Fast send path.** Packets go out through cached `MethodHandle`s (with a reflection fallback).
- **No busy loops.** Nothing polls; work is event- and scheduler-driven, and refresh loops stop the
  instant a board closes or a player leaves.

Practical guidance: pick a `refreshEvery(...)` that matches your content — `2–4` ticks for smooth
animations, `10–20` for mostly-static boards. Static content isn't refreshed at all.

### Observability

`board.stats()` returns a snapshot for profiling TPS impact:

```java
FoliaBoardStats s = board.stats();
// s.totalPackets(), s.providerRefreshes(), s.activeSidebars(), s.activeNametags()
getLogger().info(s.toString());
```

FoliaBoard also warns (once) when a board exceeds the 15-line client limit or a single line/title is
unusually large (likely accidental payload bloat).

---

## Remembering a player's layout

Optionally persist which layout a player was on, so it's re-applied on their next join with no
join-listener glue. Back the store with anything (a map, a config, a database, a storage plugin):

```java
board.setLayoutStore(new LayoutStore() {
    public CompletableFuture<Void> remember(UUID player, String layout) { return db.putAsync(player, layout); }
    public CompletableFuture<String> lastLayout(UUID player) { return db.getAsync(player); }
});
```

When set, `applyLayout(...)` records the layout name, and FoliaBoard re-applies it on join (unless a
global or per-world layout already drives that player's board).

---

### Observability

`board.stats()` returns a snapshot for profiling TPS impact:

```java
FoliaBoardStats s = board.stats();
// s.totalPackets(), s.providerRefreshes(), s.activeSidebars(), s.activeNametags()
getLogger().info(s.toString());
```

FoliaBoard also warns (once) when a board exceeds the 15-line client limit or a single line/title is
unusually large (likely accidental payload bloat).

---

## Remembering a player's layout

Optionally persist which layout a player was on, so it's re-applied on their next join with no
join-listener glue. Back the store with anything (a map, a config, a database, a storage plugin):

```java
board.setLayoutStore(new LayoutStore() {
    public void remember(UUID player, String layout) { db.put(player, layout); }
    public CompletableFuture<String> lastLayout(UUID player) { return db.getAsync(player); }
});
```

When set, `applyLayout(...)` records the layout name, and FoliaBoard re-applies it on join (unless a
global or per-world layout already drives that player's board).

---

## Lifecycle, cleanup & `/reload`

- Create once in `onEnable`, call `board.close()` in `onDisable`. `close()` cancels every task,
  unregisters the listener, and tears down all boards, nametags and objectives.
- **Auto-cleanup** on quit, world-change and plugin-disable — no ghost players, no leaks.
- **`/reload` is discouraged** (on Paper/Folia generally). Because FoliaBoard registers a listener and
  scheduler tasks, prefer a full restart. A clean disable→enable cycle won't leak (guarded against
  scheduling while disabled), but `/reload` remains unsupported as a reload mechanism.

---

## API reference

| Type | Key members |
|---|---|
| `FoliaBoard` | `create(plugin)`, `createBoard(p)`, `createNametag(p)`, `sidebar(p)`, `removeSidebar(p)`, `setGlobalSidebar(provider\|layout)`, `clearGlobalSidebar()`, `registerLayout/unregisterLayout/layout/applyLayout`, `setWorldLayout/clearWorldLayout`, `setLayoutStore`, `nametag(p)`, `belowName()`, `tabList()`, `tabName/resetTabName/tabOrder`, `tabNameFor/resetTabNameFor/perViewerTabSupported`, `tabHeaderFooter/clearTabHeaderFooter`, `addLineProcessor`, `placeholders()`, `stats()`, `close()` |
| `ScoreboardAPI` | `init(plugin)`, `get()`, `shutdown()`, `createBoard(p)`, `createNametag(p)`, `sidebar(p)` |
| `BoardBuilder` | `placeholders(bool)`, `refreshEvery(ticks)`, `title(...)`, `line(...)`, `lines(...)`, `blankLine()`, `build()` |
| `Sidebar` | `title(...)`, `line(...)`, `lines(...)`, `removeLine`, `clearLines`, `visible(...)`, `title()`, `lines()`, `lineCount()`, `close()` |
| `NametagBuilder` | `prefix/suffix/color/nametagVisibility/collision`, `tabSort(int)`, `perViewer(resolver)`, `apply()` |
| `Nametag` | `prefix/suffix/color/…`, `perViewer(resolver)`, `apply()`, `remove()` |
| `ScoreObjective` | `title(...)`, `score(player\|entry, v)`, `remove(entry)`, `scoreFor/removeFor(viewer,…)`, `hide()`, `show()` |
| `SidebarProvider` | `title(p)`, `lines(p)`, `visible(p)`, `refreshIntervalTicks()`, `of(...)` |
| `Layout` | `named(name, recipe)`, `applyTo(board, p)` |
| `NumberFormat` | `blank()`, `fixed(c)`, `styled(style)`, `defaultFormat()` |
| `Animations` | `cycle`, `scrollText`, `pulseColor`, `typewriter`, `mini` |
| `Placeholders` | `register(...)`, `apply(p,text)`, `component(p,text)` |
| `Text` | `mini(...)`, `toMini(c)` |
| `AsyncUtil` | `async`, `asyncLater`, `onPlayer`, `global`, `isFolia` |
| events / hooks | `SidebarCreateEvent`, `LayoutApplyEvent`, `LineProcessor` |

---

## Version support & the honest caveat

- Requires **Paper/Folia 1.20.6+**, **Java 21**. Targets the Mojang-mapped runtime and modern
  per-score display-component packets. **Validated live end-to-end on Folia 1.21.11.**
- The packet layer (`NmsPacketAdapter`) reaches into server internals by reflection, and adapts at
  load to whether score-packet fields are `Optional<…>` or `@Nullable`. It's the **only**
  version-specific file, and it fails **loudly at load** (never mid-game) if a handle can't resolve.
  If a future Minecraft release moves a field or changes a packet's shape, that one file is where you
  adjust it.

---

## Building from source

```bash
mvn clean package
```

- `foliaboard-core/target/foliaboard-core-1.0.0.jar` — the library (depend on this).
- `foliaboard-demo/target/FoliaBoard-1.0.0.jar` — a standalone demo plugin. Drop it into `plugins/`,
  join, and use `/fbdemo lobby|minigame` to see everything at once. Add `-Dfoliaboard.debug=true` to
  log every scoreboard packet.

See [`foliaboard-demo/.../ExamplePlugin.java`](foliaboard-demo/src/main/java/net/foliaboard/example/ExamplePlugin.java)
for a complete, runnable example.
#
