this is a java project
1) do not use git commands
2) always make helper methods and classes for single source of truth
3) dont keep adding stuff to a single file, cuz if possible try to make new files to keep maintainable code


how to navigate this project

---

# 1. Project Root & Bootstrap
**Package:** `rip.diamond.practice`

*   **`Eden.java` (Main Class):**
    *   **Lifecycle:** `onEnable()` initializes `SpigotAPI`, loads all configuration files, managers, listeners, and commands. `onDisable()` handles data saving (Profiles, Kits, Arenas) and world restoration.
    *   **Singleton:** Accessible via `Eden.INSTANCE`.
    *   **Cache:** `EdenCache` tracks real-time player counts (online, queue, match) to avoid calculating streams on every tick.
    *   **Placeholders (Two Systems):**
        *   `EdenPlaceholder` handles internal replacement (`{queue-time}`, `{match-duration}`).
        *   `EdenPlaceholderExpansion` handles PlaceholderAPI (`%eden_player_wins%`) for external plugins.
        *   **Note:** Some menus (e.g., `KitStatsMenu`) have their own `replacePlaceholders()` methods.

*   **`EdenItems.java`:**
    *   Manages "Hotbar Items" given to players in the Lobby/Queue/Spectator mode.
    *   Items are defined in `items.yml` and loaded into static `EdenItem` objects.
    *   Handles `NBT` injection for command execution on click.

---

# 2. Configuration System
The plugin uses a multi-file configuration system managed in `rip.diamond.practice.config`.

### Configuration Files & Classes
| File | Class | Purpose |
| :--- | :--- | :--- |
| `config.yml` | `Config.java` (Enum) | Global boolean/int settings (Physics, Knockback profiles, Build heights). |
| `database.yml` | `DatabaseConfig.java` | Credentials for MongoDB, MySQL, or FlatFile storage toggle. |
| `language.yml` | `Language.java` | All chat messages. Supports PlaceholderAPI. |
| `scoreboard.yml` | `ScoreboardFile.java` | Layouts for Scoreboard titles/lines per `PlayerState`. |
| `menus.yml` | `MenusConfig.java` | GUI layouts (slots, materials, titles) for Kits, Queues, and Settings. |
| `items.yml` | `EdenItems.java` | Definitions for lobby hotbar items. |
| `locations.yml` | `BasicConfigFile` | **CRITICAL.** Stores all global spawn points (Lobby, Editor, Event Spawns). |
| `arena.yml` | `BasicConfigFile` | Serialized `Arena` objects and their `ArenaDetail` snapshots. |
| `kit.yml` | `BasicConfigFile` | Serialized `Kit` objects and `KitGameRules`. |
| `chest.yml` | `BasicConfigFile` | Loot tables for SkyWars chests (RNG based). |
| `eventloadouts.yml`| `EventLoadoutsFile`| Specific inventories for events (e.g., OITC bow/arrow, Sumo items). |
| `sound.yml` | `EdenSound.java` | Sound effect mappings (e.g., `receive-duel-request`). |
| `leaderboards.yml`| `BasicConfigFile` | Hologram update times and line formatting. |
| `titles.yml` | `TitleManager.java` | Win-based player titles (Bronze→Master). |

---

# 3. Data Management Architecture
**Package:** `rip.diamond.practice.database` & `rip.diamond.practice.profile`

### A. Player Data (`PlayerProfile`)
*   **State Machine (`PlayerState`):** Controls player interaction capabilities.
    *   `LOADING`: Database sync.
    *   `IN_LOBBY`: Spawn logic, hotbar items.
    *   `IN_QUEUE`: Waiting logic.
    *   `IN_MATCH`: Active fighting.
    *   `IN_SPECTATING`: Invisible, flight enabled.
    *   `IN_EDIT`: Kit Editor mode.
    *   `IN_EVENT`: Participating in a hosted event.
*   **Stats (`ProfileKitData`):** A Map `<KitName, Data>` storing Elo, Wins, Losses, and 8 custom `KitLoadout` slots per kit.
    *   **Note:** `getWon()` returns combined ranked+unranked wins for a kit.
*   **Settings (`ProfileSettings`):** Map of `Option` objects (Time, Scoreboard toggle, Ping range, etc.).

### B. Database Handler (`DatabaseHandler` Interface)
*   **Implementations:** `MongoHandler`, `MySqlHandler` (HikariCP), `FlatFileHandler` (JSON in `/data/`).
*   **Migration:** `EdenCommand` contains logic to move data between storage types.

---

# 4. Arena System
**Package:** `rip.diamond.practice.arenas`

*   **`Arena`:** The "Definition" of a map.
    *   **Properties:** Icon, Build Max height, Portal Protection radius.
    *   **Parkour Checkpoints:** Stored in `parkourCheckpointsA` and `parkourCheckpointsB` lists. This allows asymmetric parkour maps (Team A vs Team B routes).
*   **`ArenaDetail`:** A physical copy/instance of an Arena.
    *   **Locations:** `A` (Spawn 1), `B` (Spawn 2), `Spectator`, `Min`, `Max`.
    *   **Chunk Management:** Uses `ArenaChunkManager` to serialize NMS chunk sections to binary files in `/cache/chunks/`.
    *   **Restoration:** `restoreChunk()` injects saved NMS sections back into the world (Fast) or uses FAWE schematics (Slow/Fallback).
*   **`ActiveArenaTracker`:** Tracks which arenas are dirty to ensure restoration on server restart.

---

# 5. Kit System
**Package:** `rip.diamond.practice.kits`

*   **`Kit`:** Represents a gamemode (NoDebuff, Sumo, BuildUHC).
*   **`KitGameRules` (Boolean Logic):**
    *   **Physics:** `noFallDamage`, `deathOnWater` (Sumo), `enderPearlCooldown`, `healthRegeneration`.
    *   **Combat:** `boxing` (No damage, hits only), `noDamage`, `sumo`, `tntsumo`.
    *   **Interaction:** `build`, `breakGoal` (BedWars/Bridge), `chestAccess`.
    *   **Items:** `dropItemWhenDie`, `getBackArrow`, `instantGapple`.
    *   **Parkour:** `parkour` (Enables checkpoint logic), `parkourCheckpointBuildRadius`.
*   **`KitLoadout`:** Stores Armor and Inventory contents as Base64 strings.

---

# 6. Match Logic (Duels)
**Package:** `rip.diamond.practice.match`

### A. Structure
*   **`Match` (Abstract):** Base logic for `SOLO`, `TEAM`, `FFA`.
*   **`MatchState`:** `STARTING` (Countdown/Freeze), `FIGHTING`, `ENDING`.
*   **`Team` & `TeamPlayer`:** Wrappers for players to handle teams, combos, hits, and "Alive" status.

### B. Mechanics (`MatchListener` & `MatchMovementHandler`)
*   **Damage:** Logic in `MatchListener#onEntityDamageByEntity`.
    *   Handles `Boxing` hit counting.
    *   Handles `Sumo` no-damage knockback.
    *   Handles `TNT` and `Fireball` custom damage/knockback scaling.
*   **Movement:** `MatchMovementHandler`.
    *   Checks `ArenaDetail` boundaries.
    *   Handles `Water` death (Sumo).
    *   Handles `Void` death (SkyWars/Parkour).
    *   **Parkour Logic:** If `KitGameRules.isParkour()` is true, falling into the void triggers `MatchRespawnTask` (teleport to last checkpoint) instead of death.
*   **Building:** Checked against `ArenaDetail#cuboid` and `KitGameRules#build`.

### C. Tasks
*   `MatchNewRoundTask`: Handles round resets (for Bridge/Sumo).
*   `MatchRespawnTask`: Handles respawn timers.
*   `MatchClearBlockTask`: Removes placed blocks after X seconds (if enabled in Kit).

---

# 7. Event System (Mass Games)
**Package:** `rip.diamond.practice.events` & `rip.diamond.practice.managers`

### A. Structure
*   **`EventManager`:** Manages the active `PracticeEvent`. Creates a dedicated world named "event".
*   **`PracticeEvent<T>` (Abstract):** Base class.
    *   `T` extends `EventPlayer`: Custom wrapper for event-specific stats (e.g., `OITCPlayer` tracks lives/streak).
*   **Implemented Games:**
    *   `Sumo`, `OITC`, `TNTTag`, `Brackets`, `LMS` (Last Man Standing), `Knockout`, `SkyWars`, `Parkour`, `Gulag`, `FourCorners`, `Thimble`, `Dropper`, `StopLight`, `Spleef`.

### B. Spawn Management (`SpawnManager`)
**Crucial:** Events rely on `locations.yml`. The `SpawnManager` maps these config keys to code.
*   **Required Locations:**
    *   **Sumo:** `sumoLocation` (Lobby), `sumoFirst`, `sumoSecond`.
    *   **Brackets:** `bracketsLocation`, `bracketsFirst`, `bracketsSecond`.
    *   **LMS/Knockout:** List of spawn points.
    *   **SkyWars/Spleef:** `Min`/`Max` points for cuboid restoration.
    *   **Parkour:** `parkourGameLocation` (Start), `parkourCheckpoints` (List).

### C. Chunk Pre-loading
*   `ChunkManager`: specifically pre-loads chunks for SkyWars, Spleef, and FourCorners regions defined in `SpawnManager` to prevent lag spikes on event start.

---

# 8. Visuals & Utilities

### A. Scoreboard (`rip.diamond.practice.util.scoreboard`)
*   **`ScoreboardHandler`:** Threaded updater.
*   **`ScoreboardAdapter`:** Determines lines based on `PlayerState`. Reads from `scoreboard.yml`.
*   **Logic:** Replaces placeholders like `{online-players}`, `{match-duration}`, `{opponent-name}`.

### B. Tablist (`rip.diamond.practice.util.tablist`)
*   **`ImanityTabHandler`:** Uses ProtocolLib to intercept packets and create a custom 4-column layout.
*   **Layout:** Defined in `Language.java` (Header/Footer) and dynamic player listing logic.

### C. Nametags (`rip.diamond.practice.util.nametags`)
*   **`NameTagManager`:** Updates prefixes/suffixes via Scoreboard Teams packets.
*   **Logic:** Color codes based on relation (Teammate = Green, Enemy = Red, Party = Blue).

### D. Menus (`rip.diamond.practice.util.menu`)
*   Abstract menu system using `InventoryClickEvent`.
*   Supports Pagination (`PaginatedMenu`).
*   **Key Menus:** `KitEditorMenu`, `ArenaEditMenu`, `EventHostMenu`.

### E. NMS Abstraction (`SpigotAPI`)
*   **Knockback:** Supports `Default`, `Carbon`, and `WindSpigot` implementations.
*   **Movement:** Packet injection for accurate movement tracking.

---

# 9. Key Feature: Custom Items
**Package:** `rip.diamond.practice.managers` -> `CustomItemManager`

*   **`DefaultItem` Enum:**
    *   `GOLDEN_HEAD`: Custom skull with regeneration effects.
    *   `INSTA_BOOM_TNT`: TNT that explodes instantly (used in TNT Sumo/Bridge).
*   **Logic:** `MatchListener` checks `isGoldenHead` or `isInstaBoomTNT` on interact/place to execute custom logic (e.g., instant explosion, potion effects).

---

# 10. Navigation Summary

| Feature | Primary Logic Location | Config File |
| :--- | :--- | :--- |
| **Duel Start/End** | `Match.java`, `QueueTask.java` | `kit.yml`, `arena.yml` |
| **Combat Logic** | `MatchListener.java` | `config.yml` (Physics) |
| **Kits/Loadouts** | `Kit.java`, `KitLoadout.java` | `kit.yml` |
| **Arena Editing** | `ArenaCommand.java`, `ArenaEditMenu.java` | `arena.yml` |
| **Events** | `PracticeEvent.java`, `EventManager.java` | `locations.yml` |
| **Chunk Restore** | `ArenaChunkManager.java` (NMS) | N/A (Binary Cache) |
| **Scoreboard** | `ScoreboardAdapter.java` | `scoreboard.yml` |
| **Messages** | `Language.java` | `language.yml` |
| **Storage** | `DatabaseManager.java` | `database.yml` |
| **Titles** | `TitleManager.java`, `Title.java` | `titles.yml` |

rules to update this workflow
1) even though multiple shorter changes may be made in the future, always update this workflow BUT DONT bias minor changes to appear bigger or more significant than they are because you just added them and understand more, mention whats necessary to navigate this project proportional to the other features compared to the new ones.
2) update this workflow immediately after a big architechtural change, wait for my confirmation if its a minor tweak
