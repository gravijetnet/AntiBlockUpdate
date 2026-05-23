# Bug Report — AntiBlockUpdate

Exhaustive audit of all Java files. Bugs are grouped by file, ordered by line number.
Severity labels: **CRITICAL** / **HIGH** / **MEDIUM** / **LOW** / **INFO**.

---

## `Main.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 1 | 449 | HIGH | Compatibility | `GRASS` (used in `onBlockSpread` switch) was renamed to `GRASS_BLOCK` in Minecraft 1.13. The `case GRASS:` literal will fail to compile or match nothing on 1.13+ servers, silently disabling grass-spread suppression. |
| 2 | 455 | HIGH | Compatibility | `MYCEL` was renamed to `MYCELIUM` in Minecraft 1.13. Same compile/match failure as above. |
| 3 | 487 | HIGH | Compatibility | `SOIL` (farmland) was renamed to `FARMLAND` in Minecraft 1.13. `onBlockFade` will never cancel farmland drying on 1.13+ servers. |
| 4 | 246 | MEDIUM | Logic | `portalEnabled` is set from `getConfig()` *after* `buildSets()` is called. `buildSets()` also reads from `getConfig()`, but if the config is not yet hydrated at that point (e.g., a race during first-run `saveDefaultConfig`), portal state could be inconsistent. The ordering dependency is fragile. |
| 5 | 255–258 | MEDIUM | NPE | `getCommand("portal")` may return `null` if the command is not declared in `plugin.yml`. The null-check only guards `setExecutor`/`setTabCompleter`, but the plugin still calls `registerOutgoingPluginChannel` unconditionally on line 259 and later tries to unregister it — meaning the channel is registered even when the command registration silently failed, which can mislead debugging. |
| 6 | 668 | MEDIUM | Logic | `onCreatureSpawn` only cancels spawns with reason `NATURAL`. Monsters spawned by spawners, eggs, or plugins are unaffected even if the admin expects "no monsters." The config key name `natural-monsters` is correct, but there is no separate option for spawner/egg monsters, making the feature incomplete for common lobby setups. |
| 7 | 682–702 | MEDIUM | Logic | In `onEntityDamageByEntity`, when the attacker is a `Projectile` but `getShooter()` returns a non-entity `ProjectileSource` (e.g., a dispenser), `attacker` remains the projectile object itself. A projectile is not a `Player` or `Monster`, so none of the combat blocks apply — dispenser-shot arrows deal full damage even when `cfgBlockPvp` is `true`. |
| 8 | 736–741 | MEDIUM | Logic | `onFoodLevelChange` blocks hunger loss but also blocks **food gain** (eating food raises `getFoodLevel`, so the condition `event.getFoodLevel() < player.getFoodLevel()` is false and eating is allowed). However, regeneration triggered by full food is a separate mechanism, so players can still lose health indirectly. The guard is subtly correct for depletion-only but is not documented, making it easy to accidentally break. |
| 9 | 750 | LOW | Logic | `onWeatherChange` cancels only transitions *to* stormy weather (`event.toWeatherState() == true`). If the server reloads mid-storm the storm will clear normally. This is acceptable behavior but is undocumented and may surprise server operators who expect persistent clear weather. |
| 10 | 391–392 | LOW | Performance | `onBlockPhysics` is called extremely frequently (every block-update tick). Using `physicsBlacklist.contains()` on a `HashSet` is O(1), but registering at `EventPriority.HIGHEST` with `ignoreCancelled = true` means it runs after every other plugin, which can add measurable overhead on busy servers when many block types are in the blacklist. No caching of the block type is done before the set lookup. |
| 11 | 114–125 | LOW | Thread-safety | `safeMaterials` is used to initialise `static final` fields. Static initializers run once at class-load time in a single thread, so there is no thread-safety issue here, but the resulting `HashSet` instances are not wrapped as unmodifiable. Any code that obtains a reference to these sets (e.g., via reflection) could mutate them. |
| 12 | 203–211 | LOW | Logic | `DOOR_MATERIALS` does not include `IRON_DOOR` (only iron door block from 1.12, `IRON_DOOR_BLOCK`, is listed under the 1.12 section, but the 1.13+ name `IRON_DOOR` is absent from `DOOR_MATERIALS`). Iron doors will not be blocked by `cfgInteractDoors` on 1.13+ servers. |
| 13 | 574–594 | LOW | Logic | `onEntityExplode` only calls `event.blockList().clear()` — the explosion still kills/damages entities. If the intent is a full explosion cancel the event should be `setCancelled(true)`. The current behavior is intentional per the comment, but the config keys are named `tnt`, `creeper`, etc. with no suffix, which may mislead admins into thinking entity damage is also blocked. |
| 14 | 127–181 | INFO | Maintainability | `ALL_PHYSICS_BLOCKS` includes both 1.12 and 1.13+ names for many materials (e.g., `RAILS`/`RAIL`, `SUGAR_CANE_BLOCK`/`SUGAR_CANE`). On a 1.13+ server, `safeMaterials` silently skips the unknown 1.12 names. This means config keys like `anti-block-update.physics.blocks.RAILS` (1.12 name) written into `config.yml` will never match the runtime `Material.RAIL` name, causing the block's physics to always be enabled regardless of the config value. |

---

## `Portal.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 15 | 63–65 | CRITICAL | Security / Command Injection | `{player_name}` is substituted with the raw player name into a console command. Player names in vanilla Minecraft are restricted to `[A-Za-z0-9_]`, but on offline-mode or proxy servers (BungeeCord) names can contain spaces or special characters. A player whose name is crafted to inject subcommands (e.g., using a semicolon if the command framework supports it) could execute arbitrary console commands. There is no sanitisation or whitelist check on the substituted value. |
| 16 | 63 | HIGH | Security | `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)` runs with full server operator privileges. If `value` is user-controllable (e.g., admins can be socially engineered into creating portals with dangerous commands), there is no privilege separation or sandboxing. |
| 17 | 72–86 | MEDIUM | Resource | `ByteArrayOutputStream` and `DataOutputStream` are created on every call to `sendToServer`. The `ByteArrayOutputStream` is small and short-lived so GC pressure is low, but on high-traffic portals this creates many ephemeral objects per player move event. |
| 18 | 41–49 | LOW | Logic | `contains()` casts `min`/`max` coordinates to `int` using `(int)`. For negative coordinates, `(int) -0.9` truncates toward zero to `0`, not `-1`. The cast should use `(int) Math.floor()` to correctly handle negative block coordinates. This can cause the portal boundary to be off by one on the negative side of any axis. |
| 19 | 103–121 | LOW | Error Handling | `deserialize` wraps `IllegalArgumentException` from the inner `PortalType.valueOf` call but re-throws it as a new `IllegalArgumentException` with a chained cause. The outer `try/catch` in `PortalManager.loadPortals` (line 107) catches `Exception`, so the portal is silently skipped with only a warning. A corrupt type field does log a message, but the log does not include the portal name prominently enough for quick diagnosis. |
| 20 | 88–100 | LOW | Correctness | `serialize()` uses `HashMap` which has no guaranteed iteration order. When `savePortals()` writes the list to YAML, field order in each portal entry is non-deterministic. This makes diffs of `portals.yml` noisy and harder to review under version control. A `LinkedHashMap` would produce stable output. |

---

## `PortalCommand.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 21 | 106–109 | MEDIUM | TOCTOU Race | `getPortal(name) != null` check (line 106) followed by `createPortalFromSelection` (line 110) is a check-then-act pattern with a window for a race condition: two players could simultaneously pass the null-check and both call `createPortalFromSelection`. The manager's `putIfAbsent` prevents a double-insert, but the second player receives the generic failure message rather than "portal already exists," which is confusing UX. |
| 22 | 98 | LOW | Logic | `String.join(" ", Arrays.copyOfRange(args, 3, args.length))` reconstructs the value from command tokens. If a player passes a value containing multiple spaces (e.g., quoting is intended), the extra spaces are collapsed. This is a known limitation of Bukkit command parsing but is undocumented, so admins may be confused when a command like `say  hello` loses its double-space. |
| 23 | 66 | LOW | UX | `cmdWand` checks permission before checking `sender instanceof Player`. Best practice is to check `instanceof Player` first (to give a sensible "console cannot use this" message), then permission. Currently a console sender receives "You do not have permission" instead of "Only players can use this command." Same issue at lines 162–163 (`cmdTest`). |
| 24 | 195–218 | LOW | Security | Tab completion for `delete` and `info` subcommands returns all portal names to any sender, regardless of permission. A player without `PERM_MANAGE` can still enumerate portal names via tab-complete even though they cannot use the command. |
| 25 | 53–61 | INFO | Maintainability | Help text hardcodes subcommand names and usage strings. If a subcommand is added or renamed, the help text must be updated manually; there is no single source of truth. |

---

## `PortalListener.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 26 | 32–59 | HIGH | Performance | `onPlayerMove` fires on every movement event, including head rotation (yaw/pitch changes). The block-coordinate equality check (lines 37–40) skips head-only moves, but the event is still instantiated and the method is still entered for every packet. On servers with many players this can cause significant overhead. The event should use `ignoreCancelled = true` and ideally a coarser movement check. |
| 27 | 45 | MEDIUM | Performance | `portalManager.getPortalAt(to)` iterates over the world-portal list on every block-crossing move. If many portals exist in the same world, this is O(n) per move event per player. There is no spatial index (e.g., chunk-based bucketing). |
| 28 | 81 | MEDIUM | Logic | The wand interaction handler only fires on `RIGHT_CLICK_BLOCK`. A player right-clicking air while holding a Blaze Rod does nothing. If the player clicks air thinking they set a point, there is no feedback that nothing was recorded. |
| 29 | 77–98 | MEDIUM | Logic | The wand interaction handler does not check whether the player has the `antiblockupdate.portal.use` or `antiblockupdate.portal.manage` permission. Any player holding a Blaze Rod can set selection points and potentially trigger `portalManager.setSelection`, storing data in the selections map indefinitely (memory leak if the player never quits or runs `/portal create`). |
| 30 | 88–93 | LOW | Logic | The cross-world check for point 2 (line 88) compares the existing selection's world (`world1`) with the current click's world. But if point 1 was never set (only point 2 exists from a previous partial selection), `getSelectionWorld` returns `null` and the check is skipped, allowing an inconsistent selection to be stored. |
| 31 | 24 | LOW | Thread-safety | `playerInPortal` is a `ConcurrentHashMap`, which is safe for concurrent reads/writes, but the check-then-act in `onPlayerMove` (get → compare → put, lines 48–54) is not atomic. Two concurrent calls for the same player (theoretically possible with async event pipelines) could both pass the `!portal.getName().equals(current)` check and execute the portal twice. |

---

## `PortalManager.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 32 | 94 | HIGH | I/O | `YamlConfiguration.loadConfiguration(portalsFile)` silently returns an empty config if the file does not exist or cannot be read. If the file was deleted between the `portalsFile.exists()` check and the `loadConfiguration` call, all portals are silently wiped from memory with no error log. |
| 33 | 95–96 | HIGH | Logic | `loadPortals()` calls `portals.clear()` and `portalsByWorld.clear()` before the new portals are parsed. If parsing fails mid-way (e.g., a corrupt entry throws an uncaught exception beyond the per-entry try/catch), the plugin ends up with a partially-loaded portal set and no indication that data is missing. |
| 34 | 77–112 | MEDIUM | I/O | `loadPortals()` reassigns `portalsFile` on every call (line 78), which is redundant since the data folder never changes at runtime. This is harmless but wasteful and could mask a future bug if the data folder were configurable. |
| 35 | 122–135 | MEDIUM | I/O | `savePortals()` overwrites the entire `portals.yml` file on every create/delete, even for a single change. On servers with many portals or slow disk I/O, this blocks the main thread and could cause TPS lag. Saves should be done asynchronously. |
| 36 | 141 | MEDIUM | Logic | `createPortal` (the overload used internally) calls `portals.containsKey(key)` then `portals.putIfAbsent(key, portal)`. The `containsKey` check is redundant with `putIfAbsent` and adds a spurious read. More importantly, `addToWorldIndex` and `savePortals` are called even when `putIfAbsent` fails (returns non-null), though in this method that cannot happen due to the earlier `containsKey` guard — the logic is confusing and inconsistent with `createPortalFromSelection`. |
| 37 | 225–228 | MEDIUM | Thread-safety | `addToWorldIndex` uses `computeIfAbsent` to create a `synchronizedList`-wrapped `ArrayList`. However, iterating over the list in `getPortalAt` (line 170) is done without synchronization. A concurrent `addToWorldIndex` or `removeFromWorldIndex` during iteration can throw `ConcurrentModificationException`. |
| 38 | 230–234 | MEDIUM | Thread-safety | `removeFromWorldIndex` calls `worldPortals.remove(portal)` on the synchronized list, but uses the `Portal.equals` method (which compares only by `name`) for removal. If two portals somehow have the same name but different objects (shouldn't happen with `putIfAbsent` enforcement), the wrong entry could be removed. |
| 39 | 184–188 | LOW | API Contract | `getSelection` returns a `Vector[2]` where either element may be `null` if only one point is set. The Javadoc warning exists, but callers outside this class that call `getSelection` directly rather than `hasCompleteSelection` first will get an NPE when using the returned vectors. |
| 40 | 98–110 | LOW | Type Safety | `portalsConfig.getMapList("portals")` returns `List<Map<?,?>>`. The inner cast to `Map<String, Object>` (via `entry.getKey().toString()`) is safe but silently coerces non-String keys. A YAML key that is an integer (e.g., `1: foo`) would be silently converted to `"1"`, which could cause unexpected field matching or data loss. |
| 41 | 204–215 | LOW | Logic | `createPortalFromSelection` calls `clearSelection(player)` only on success. On failure (name already exists or cross-world selection), the player's selection is preserved. This is arguably correct behavior (let them fix the name and retry), but it is undocumented and may confuse players who expect the wand to reset after any attempt. |

---

## `PortalType.java`

| # | Line(s) | Severity | Category | Description |
|---|---------|----------|----------|-------------|
| 42 | 1–6 | INFO | Extensibility | `PortalType` has only `SERVER` and `COMMAND`. There is no `WARP` (teleport within the same server) type. The enum is not designed for extension (no abstract method or interface), so adding a new type requires modifying both the enum and the `Portal.execute` switch. Not a bug, but a design limitation. |

---

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 1 |
| HIGH | 7 |
| MEDIUM | 15 |
| LOW | 14 |
| INFO | 4 |
| **Total** | **41** |
