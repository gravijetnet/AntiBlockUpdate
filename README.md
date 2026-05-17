# AntiBlockUpdate

A Minecraft plugin that gives server administrators fine-grained control over block updates, physics events, mob behavior, player interactions, and more. Every feature is independently toggleable via `config.yml`.

---

## Commands

### `/portal`

Manage portal regions that teleport players to other BungeeCord servers or execute commands when entered.

**Permission:** `antiblockupdate.portal`

| Subcommand | Usage | Description |
|---|---|---|
| `wand` | `/portal wand` | Receive the selection wand (Blaze Rod) |
| `create` | `/portal create <name> <type> <value>` | Create a portal from your current selection |
| `delete` | `/portal delete <name>` | Delete a portal by name |
| `list` | `/portal list` | List all existing portals |
| `info` | `/portal info <name>` | Show details of a specific portal |
| `reload` | `/portal reload` | Reload portal data from `portals.yml` |
| `test` | `/portal test` | Check whether you are currently inside a portal region |

#### Portal Types

| Type | Value | Behavior |
|---|---|---|
| `SERVER` | Server name | Sends the player to that BungeeCord server |
| `COMMAND` | Console command | Runs the command as console; `{player}` is replaced with the player's name |

#### Creating a Portal

1. Run `/portal wand` to get the Blaze Rod.
2. **Right-click** a block to set point 1.
3. **Sneak + right-click** a block to set point 2.
4. Run `/portal create <name> <type> <value>` to save the region.

Portals are saved to `portals.yml` and persist across restarts.

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `antiblockupdate.portal` | Allows use of all `/portal` subcommands | op |

---

## Features

All features are configured in `config.yml` under the `anti-block-update` section. Features marked **enabled by default** are active on a fresh install; all others are disabled unless turned on.

### Block Physics

Prevents blocks from updating, falling, or disappearing due to physics. Can be toggled globally or per block type.

Supported block types include: sand, gravel, anvil, dragon egg, torches, levers, buttons, signs, ladders, vines, crops, rails, doors, gates, pressure plates, fire, snow, and more.

### Falling Blocks

Prevents falling blocks (sand, gravel, anvils, etc.) from landing and converting the block below.

### Fire

| Option | Default | Description |
|---|---|---|
| Spread | enabled | Fire cannot spread to adjacent blocks |
| Burn | enabled | Blocks cannot be destroyed by fire |
| Extinguish | disabled | Fire cannot go out naturally |
| Ignition — spread | enabled | Prevents fire spreading from burning blocks |
| Ignition — lava | enabled | Lava cannot ignite nearby blocks |
| Ignition — flint & steel | disabled | Players can still ignite blocks |
| Ignition — lightning | disabled | Lightning can still ignite blocks |
| Ignition — fireball | disabled | Fireballs can still ignite blocks |
| Ignition — explosion | disabled | Explosions can still ignite blocks |

### Block Spread

| Option | Default | Description |
|---|---|---|
| Grass | enabled | Grass cannot spread to dirt |
| Mycelium | enabled | Mycelium cannot spread |
| Mushrooms | enabled | Mushrooms cannot spread |
| Vines | enabled | Vines cannot grow |

### Ice & Snow

| Option | Default | Description |
|---|---|---|
| Ice melt | enabled | Ice cannot melt |
| Ice form | disabled | Water cannot freeze |
| Snow melt | enabled | Snow layers cannot melt |
| Snow form | disabled | Snow cannot accumulate |

### Farmland

When enabled, farmland cannot dry out and turn to dirt.

### Block Formation

| Option | Default | Description |
|---|---|---|
| Obsidian | disabled | Lava + water cannot form obsidian |
| Cobblestone | disabled | Lava + water cannot form cobblestone |

### Plant Growth

Prevents specific plants from growing. Per-plant control for: wheat, carrots, potatoes, melon stems, pumpkin stems, nether wart, cocoa, cactus, and sugar cane.

### Liquid Flow

| Option | Default | Description |
|---|---|---|
| Water | disabled | Water cannot flow |
| Lava | disabled | Lava cannot flow |

### Dragon Egg

**Enabled by default.** The dragon egg cannot teleport when clicked.

### Leaves Decay

**Enabled by default.** Leaves do not decay when disconnected from logs.

### Structure Growth

| Option | Default | Description |
|---|---|---|
| Trees | disabled | Saplings cannot grow into trees |
| Giant mushrooms | disabled | Mushrooms cannot grow into giant mushrooms |

### Entity Block Changes

| Option | Default | Description |
|---|---|---|
| Endermen | enabled | Endermen cannot pick up or place blocks |
| Sheep | enabled | Sheep cannot eat grass (grass stays, does not turn to dirt) |
| Silverfish | enabled | Silverfish cannot enter or exit stone blocks |
| Wither | enabled | The Wither cannot destroy blocks |
| Zombie doors | disabled | Zombies can still break wooden doors |

### Explosions

Block damage from explosions can be disabled per source:

- TNT, Creepers, Wither, Fireballs, Wither Skulls

All disabled by default.

### Pistons

| Option | Default | Description |
|---|---|---|
| Extend | disabled | Pistons cannot extend |
| Retract | disabled | Pistons cannot retract |

### Redstone

**Disabled by default.** When enabled, all redstone current changes are frozen (redstone stops working entirely).

### Dispensers & Droppers

**Disabled by default.** Dispensers and droppers cannot dispense items.

### Block Interaction

| Option | Default | Description |
|---|---|---|
| Block break | disabled | Players cannot break blocks |
| Block place | disabled | Players cannot place blocks |
| Block damage | disabled | Breaking animation is suppressed |

### Bucket Interactions

| Option | Default | Description |
|---|---|---|
| Fill | disabled | Players cannot fill buckets from water/lava |
| Empty | disabled | Players cannot empty buckets into the world |

### Player Block Interactions (right-click)

Fine-grained control over which blocks players can interact with:

| Option | Default |
|---|---|
| Beds | enabled (sleep blocked) |
| Doors, trapdoors, fence gates | disabled |
| Buttons (stone & wood) | disabled |
| Levers | disabled |
| Pressure plates | disabled |
| Note blocks | disabled |
| Jukeboxes | disabled |
| Chests & ender chests | disabled |
| Furnaces | disabled |
| Crafting tables | disabled |
| Enchanting tables | disabled |
| Anvils | disabled |
| Brewing stands | disabled |
| Beacons | disabled |
| Dispensers & droppers | disabled |
| Hoppers | disabled |

### Mob Spawning

Controls natural (random tick) spawning only — spawners and manual spawning are unaffected.

| Option | Default | Description |
|---|---|---|
| Passive mobs | enabled | Animals cannot spawn naturally |
| Monsters | enabled | Hostile mobs cannot spawn naturally |

### Combat

| Option | Default | Description |
|---|---|---|
| PvP | disabled | Players cannot damage each other |
| Hit passive mobs | disabled | Players cannot hit passive mobs |
| Hit monsters | disabled | Players cannot hit monsters |
| Monster damage | disabled | Monsters cannot damage players |
| Fall damage | disabled | Players take no fall damage |
| Void damage | disabled | Players do not die in the void |

### Item Handling

| Option | Default | Description |
|---|---|---|
| Item drop | disabled | Players cannot drop items |
| Item pickup | disabled | Players cannot pick up items |

### Hunger

**Enabled by default.** The hunger bar does not deplete. Players can still eat to restore hunger, but it will not drain passively.

### Weather

**Enabled by default.** The weather cannot change — rain, storms, and clear skies are locked in their current state.

### Portals

**Enabled by default.** Enables the portal region system (see [Commands](#commands) above). Can be disabled entirely by setting `portal.enabled: false` in `config.yml`.

---

## Configuration Files

| File | Purpose |
|---|---|
| `config.yml` | All feature toggles and settings |
| `portals.yml` | Saved portal regions (auto-managed) |
