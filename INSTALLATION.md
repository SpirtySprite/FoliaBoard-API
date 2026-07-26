# Installing FoliaBoard

There are **two ways** to use FoliaBoard. Pick one.

| Module | Artifact | Use it when |
|---|---|---|
| `foliaboard-core` | `net.foliaboard:foliaboard-core:1.0.0` | You want to **shade** the library into your own plugin (self-contained, no extra install). Ships **no `plugin.yml`**. |
| `foliaboard-plugin` | `FoliaBoard-1.0.0.jar` | You want to **install one shared plugin** on the server (the Modrinth download) and have your plugins `softdepend` on it. |
| `foliaboard-demo` | `FoliaBoard-Demo-1.0.0.jar` | Just a runnable example. Not a dependency, not for production. |

- **Model A — shade** (`foliaboard-core`): everything is inside your jar; nothing else to install.
- **Model B — install + softdepend** (`foliaboard-plugin`): one FoliaBoard jar on the server, many
  plugins share its API. This is what you publish on Modrinth.

The `foliaboard-core` artifact deliberately ships **no `plugin.yml`**, so shading it can't clobber
yours. The standalone `foliaboard-plugin` is the only one with a `plugin.yml`.

---

## Model B — install the standalone plugin (Modrinth)

1. Build (or download from Modrinth) `foliaboard-plugin/target/FoliaBoard-1.0.0.jar` and drop it in
   the server's `plugins/`.
2. In **your** plugin, add FoliaBoard as a **provided** dependency (compile against it, don't bundle):
   ```xml
   <dependency>
     <groupId>net.foliaboard</groupId>
     <artifactId>foliaboard-core</artifactId>
     <version>1.0.0</version>
     <scope>provided</scope>
   </dependency>
   ```
3. Declare the soft-dependency in your `plugin.yml`:
   ```yaml
   softdepend: [FoliaBoard]
   ```
4. Use the shared API:
   ```java
   import net.foliaboard.api.ScoreboardAPI;
   // (FoliaBoard's own plugin already called ScoreboardAPI.init)
   ScoreboardAPI.get().createBoard(player).title("<aqua>Hi").build();
   ```
   Do **not** call `ScoreboardAPI.init/shutdown` yourself in this model — the FoliaBoard plugin owns
   the lifecycle.

`/foliaboard` (permission `foliaboard.admin`) prints the version and API status.

---

## Model A — shade the library into your plugin

## Requirements

- Paper or **Folia 1.20.6+** (validated live on 1.21.11)
- **Java 21**

## 1. Build it

```bash
mvn clean package
```

Outputs:
- `foliaboard-core/target/foliaboard-core-1.0.0.jar` — the library (depend on this)
- `foliaboard-demo/target/FoliaBoard-1.0.0.jar` — the runnable demo plugin

Install the library to your local Maven repo so other projects can depend on it:

```bash
mvn -pl foliaboard-core install
```

## 2. Depend on it and shade it

In **your plugin's** `pom.xml`:

```xml
<dependency>
    <groupId>net.foliaboard</groupId>
    <artifactId>foliaboard-core</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

Shade + **relocate** it so it can't clash with other plugins that bundle it:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>net.foliaboard</pattern>
                        <shadedPattern>com.yourplugin.libs.foliaboard</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Because the library has no `plugin.yml`, **your** `plugin.yml` is the only one in the final jar.

## 3. Make YOUR plugin Folia-ready

Your own `plugin.yml` **must** declare Folia support, or the server won't load it on Folia:

```yaml
name: YourPlugin
main: com.yourplugin.YourPlugin
version: 1.0.0
api-version: '1.20'
folia-supported: true
softdepend: [PlaceholderAPI]   # optional, enables the placeholder bridge
```

## 4. Use it

```java
public final class YourPlugin extends JavaPlugin {
    private FoliaBoard board;

    @Override
    public void onEnable() {
        board = FoliaBoard.create(this);
        // ... createBoard / setGlobalSidebar / layouts ...
    }

    @Override
    public void onDisable() {
        if (board != null) board.close();   // always close in onDisable
    }
}
```

## Running the bundled demo (optional)

Drop `foliaboard-demo/target/FoliaBoard-1.0.0.jar` into `plugins/`, start a 1.20.6+ Paper/Folia
server, and join. On join you get a plain sidebar; `/fbtest <1-7>` demonstrates each feature
(MiniMessage, number formats, nametags, below-name, tab, and a full combined board). Start the server
with `-Dfoliaboard.debug=true` to log every scoreboard packet as it is sent.

## A note on `/reload`

`/reload` (and plugin managers that disable/enable plugins at runtime) is **not** a supported way to
reload FoliaBoard. Because FoliaBoard registers a listener and region-scheduler tasks, always pair
`FoliaBoard.create(this)` in `onEnable` with `board.close()` in `onDisable`, and prefer a full server
restart over `/reload`. `close()` cancels all tasks, unregisters the listener, and tears down every
board, so a clean disable/enable cycle won't leak or double-register — but `/reload` remains
discouraged on Paper/Folia generally.

## Troubleshooting

- **"requires Minecraft 1.20.6 or newer"** — FoliaBoard's packet layer targets modern versions only.
- **"failed to initialise the packet adapter"** at load — your server's internals differ from the
  1.20.6–1.21.x mappings FoliaBoard targets. All reflection lives in
  `internal/packet/reflect/NmsPacketAdapter.java`; adjust the class/field/method names there. It fails
  at load (not mid-game) on purpose.
- **PlaceholderAPI placeholders not resolving** — add `PlaceholderAPI` to your `softdepend` and make
  sure it's installed; `board.placeholders().placeholderApiPresent()` tells you if the bridge is live.
