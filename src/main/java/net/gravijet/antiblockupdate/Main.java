package net.gravijet.antiblockupdate;

import net.gravijet.lobby.portal.*;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private PortalManager portalManager;
    private Set<Material> physicsBlacklist;
    private Set<Material> plantBlacklist;

    private static final Set<Material> ALL_PHYSICS_BLOCKS = EnumSet.of(
            Material.SAND, Material.GRAVEL, Material.ANVIL, Material.DRAGON_EGG,
            Material.TORCH, Material.LEVER, Material.STONE_BUTTON, Material.WOOD_BUTTON,
            Material.TRIPWIRE_HOOK, Material.TRIPWIRE, Material.SIGN_POST, Material.WALL_SIGN,
            Material.LADDER, Material.VINE, Material.YELLOW_FLOWER, Material.RED_ROSE,
            Material.LONG_GRASS, Material.DEAD_BUSH, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM,
            Material.CACTUS, Material.SUGAR_CANE_BLOCK, Material.WHEAT, Material.CARROT,
            Material.POTATO, Material.NETHER_WARTS, Material.COCOA, Material.PUMPKIN_STEM,
            Material.MELON_STEM, Material.PUMPKIN, Material.MELON_BLOCK,
            Material.RAILS, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL,
            Material.WOODEN_DOOR, Material.IRON_DOOR_BLOCK, Material.TRAP_DOOR,
            Material.STONE_PLATE, Material.WOOD_PLATE, Material.IRON_PLATE, Material.GOLD_PLATE,
            Material.FENCE_GATE, Material.FIRE, Material.SNOW
    );

    private static final Set<Material> ALL_PLANT_BLOCKS = EnumSet.of(
            Material.WHEAT, Material.CARROT, Material.POTATO,
            Material.MELON_STEM, Material.PUMPKIN_STEM,
            Material.NETHER_WARTS, Material.COCOA,
            Material.CACTUS, Material.SUGAR_CANE_BLOCK
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        buildSets();
        getServer().getPluginManager().registerEvents(this, this);

        if (getConfig().getBoolean("portal.enabled", true)) {
            portalManager = new PortalManager(this);
            getServer().getPluginManager().registerEvents(new PortalListener(this, portalManager), this);
            getCommand("portal").setExecutor(new PortalCommand(portalManager));
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        getLogger().info("AntiBlockUpdate enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiBlockUpdate disabled.");
    }

    private void buildSets() {
        physicsBlacklist = EnumSet.noneOf(Material.class);
        for (Material mat : ALL_PHYSICS_BLOCKS) {
            if (getConfig().getBoolean("anti-block-update.physics.blocks." + mat.name(), true))
                physicsBlacklist.add(mat);
        }

        plantBlacklist = EnumSet.noneOf(Material.class);
        for (Material mat : ALL_PLANT_BLOCKS) {
            if (getConfig().getBoolean("anti-block-update.plant-growth.plants." + mat.name(), true))
                plantBlacklist.add(mat);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PHYSICS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (getConfig().getBoolean("anti-block-update.physics.enabled", true)
                && physicsBlacklist.contains(event.getBlock().getType()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY BLOCK CHANGES
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        switch (event.getEntityType()) {
            case FALLING_BLOCK:
                if (getConfig().getBoolean("anti-block-update.falling-blocks", true))
                    event.setCancelled(true);
                break;
            case ENDERMAN:
                if (getConfig().getBoolean("anti-block-update.entity-block.enderman", true))
                    event.setCancelled(true);
                break;
            case SHEEP:
                if (getConfig().getBoolean("anti-block-update.entity-block.sheep-eat-grass", true))
                    event.setCancelled(true);
                break;
            case SILVERFISH:
                if (getConfig().getBoolean("anti-block-update.entity-block.silverfish", true))
                    event.setCancelled(true);
                break;
            case WITHER:
                if (getConfig().getBoolean("anti-block-update.entity-block.wither", true))
                    event.setCancelled(true);
                break;
            case ZOMBIE:
                if (getConfig().getBoolean("anti-block-update.entity-block.zombie-break-door", false))
                    event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FIRE — burn, ignition
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (getConfig().getBoolean("anti-block-update.fire.burn", true))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!getConfig().getBoolean("anti-block-update.fire.ignition.enabled", true)) return;
        if (getConfig().getBoolean("anti-block-update.fire.ignition.causes." + event.getCause().name(), false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK SPREAD — fire, grass, mycelium, mushroom, vine
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Material newType = event.getNewState().getType();
        switch (newType) {
            case FIRE:
                if (getConfig().getBoolean("anti-block-update.fire.spread", true))
                    event.setCancelled(true);
                break;
            case GRASS:
                if (getConfig().getBoolean("anti-block-update.spread.grass", true))
                    event.setCancelled(true);
                break;
            case MYCELIUM:
                if (getConfig().getBoolean("anti-block-update.spread.mycelium", true))
                    event.setCancelled(true);
                break;
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
                if (getConfig().getBoolean("anti-block-update.spread.mushroom", true))
                    event.setCancelled(true);
                break;
            case VINE:
                if (getConfig().getBoolean("anti-block-update.spread.vine", true))
                    event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK FADE — ice melt, snow melt, fire extinguish, farmland dry
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        switch (event.getBlock().getType()) {
            case ICE:
                if (getConfig().getBoolean("anti-block-update.ice.melt", true))
                    event.setCancelled(true);
                break;
            case SNOW:
                if (getConfig().getBoolean("anti-block-update.snow.melt", true))
                    event.setCancelled(true);
                break;
            case FIRE:
                if (getConfig().getBoolean("anti-block-update.fire.extinguish", false))
                    event.setCancelled(true);
                break;
            case SOIL:
                if (getConfig().getBoolean("anti-block-update.farmland-dry", false))
                    event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK FORM — snow form, ice form, obsidian, cobblestone
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        switch (event.getNewState().getType()) {
            case SNOW:
                if (getConfig().getBoolean("anti-block-update.snow.form", false))
                    event.setCancelled(true);
                break;
            case ICE:
                if (getConfig().getBoolean("anti-block-update.ice.form", false))
                    event.setCancelled(true);
                break;
            case OBSIDIAN:
                if (getConfig().getBoolean("anti-block-update.block-form.obsidian", false))
                    event.setCancelled(true);
                break;
            case COBBLESTONE:
                if (getConfig().getBoolean("anti-block-update.block-form.cobblestone", false))
                    event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLANT GROWTH
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (getConfig().getBoolean("anti-block-update.plant-growth.enabled", true)
                && plantBlacklist.contains(event.getBlock().getType()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LIQUID FLOW + DRAGON EGG TELEPORT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        if ((type == Material.WATER || type == Material.STATIONARY_WATER)
                && getConfig().getBoolean("anti-block-update.liquid-flow.water", false)) {
            event.setCancelled(true);
        } else if ((type == Material.LAVA || type == Material.STATIONARY_LAVA)
                && getConfig().getBoolean("anti-block-update.liquid-flow.lava", false)) {
            event.setCancelled(true);
        } else if (type == Material.DRAGON_EGG
                && getConfig().getBoolean("anti-block-update.dragon-egg-teleport", true)) {
            event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LEAVES DECAY
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (getConfig().getBoolean("anti-block-update.leaves-decay", true))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STRUCTURE GROWTH — trees & giant mushrooms
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        TreeType species = event.getSpecies();
        if (species == TreeType.RED_MUSHROOM || species == TreeType.BROWN_MUSHROOM) {
            if (getConfig().getBoolean("anti-block-update.structure-grow.giant-mushroom", false))
                event.setCancelled(true);
        } else {
            if (getConfig().getBoolean("anti-block-update.structure-grow.tree", false))
                event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXPLOSIONS — block damage only (entity damage is unaffected)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        switch (event.getEntityType()) {
            case PRIMED_TNT:
                if (getConfig().getBoolean("anti-block-update.explosions.tnt", false))
                    event.blockList().clear();
                break;
            case CREEPER:
                if (getConfig().getBoolean("anti-block-update.explosions.creeper", false))
                    event.blockList().clear();
                break;
            case WITHER:
                if (getConfig().getBoolean("anti-block-update.explosions.wither", false))
                    event.blockList().clear();
                break;
            case FIREBALL:
            case SMALL_FIREBALL:
                if (getConfig().getBoolean("anti-block-update.explosions.fireball", false))
                    event.blockList().clear();
                break;
            case WITHER_SKULL:
                if (getConfig().getBoolean("anti-block-update.explosions.wither-skull", false))
                    event.blockList().clear();
                break;
            default:
                break;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PISTONS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (getConfig().getBoolean("anti-block-update.pistons.extend", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (getConfig().getBoolean("anti-block-update.pistons.retract", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REDSTONE  (not Cancellable — reset current to freeze it)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (getConfig().getBoolean("anti-block-update.redstone-change", false))
            event.setNewCurrent(event.getOldCurrent());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DISPENSERS & DROPPERS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (getConfig().getBoolean("anti-block-update.dispenser-dispense", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK BREAK / PLACE / DAMAGE
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (getConfig().getBoolean("anti-block-update.block-break", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (getConfig().getBoolean("anti-block-update.block-place", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (getConfig().getBoolean("anti-block-update.block-damage", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUCKETS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (getConfig().getBoolean("anti-block-update.buckets.fill", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (getConfig().getBoolean("anti-block-update.buckets.empty", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MOB SPAWNING
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (event.getEntity() instanceof Monster) {
            if (getConfig().getBoolean("anti-block-update.mob-spawn.natural-monsters", false))
                event.setCancelled(true);
        } else {
            if (getConfig().getBoolean("anti-block-update.mob-spawn.natural-mobs", false))
                event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMBAT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof Player) {
                if (getConfig().getBoolean("anti-block-update.combat.block-pvp", false))
                    event.setCancelled(true);
            } else if (event.getEntity() instanceof Monster) {
                if (getConfig().getBoolean("anti-block-update.combat.block-hit-monsters", false))
                    event.setCancelled(true);
            } else {
                if (getConfig().getBoolean("anti-block-update.combat.block-hit-mobs", false))
                    event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Player && event.getDamager() instanceof Monster) {
            if (getConfig().getBoolean("anti-block-update.combat.block-monster-damage", false))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && getConfig().getBoolean("anti-block-update.combat.block-fall-damage", false))
            event.setCancelled(true);
        else if (event.getCause() == EntityDamageEvent.DamageCause.VOID
                && getConfig().getBoolean("anti-block-update.combat.block-void-damage", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ITEM HANDLING
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (getConfig().getBoolean("anti-block-update.item-drop", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (getConfig().getBoolean("anti-block-update.item-pickup", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HUNGER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() < player.getFoodLevel()
                && getConfig().getBoolean("anti-block-update.hunger-depletion", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WEATHER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (getConfig().getBoolean("anti-block-update.weather-change", false))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLAYER INTERACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();

        if (type == Material.BED_BLOCK) {
            if (getConfig().getBoolean("anti-block-update.interact.bed", true))
                event.setCancelled(true);
        } else if (type == Material.WOODEN_DOOR || type == Material.TRAP_DOOR || type == Material.FENCE_GATE) {
            if (getConfig().getBoolean("anti-block-update.interact.doors", false))
                event.setCancelled(true);
        } else if (type == Material.STONE_BUTTON || type == Material.WOOD_BUTTON) {
            if (getConfig().getBoolean("anti-block-update.interact.buttons", false))
                event.setCancelled(true);
        } else if (type == Material.LEVER) {
            if (getConfig().getBoolean("anti-block-update.interact.lever", false))
                event.setCancelled(true);
        } else if (type == Material.STONE_PLATE || type == Material.WOOD_PLATE
                || type == Material.IRON_PLATE || type == Material.GOLD_PLATE) {
            if (getConfig().getBoolean("anti-block-update.interact.pressure-plates", false))
                event.setCancelled(true);
        } else if (type == Material.NOTE_BLOCK) {
            if (getConfig().getBoolean("anti-block-update.interact.note-block", false))
                event.setCancelled(true);
        } else if (type == Material.JUKEBOX) {
            if (getConfig().getBoolean("anti-block-update.interact.jukebox", false))
                event.setCancelled(true);
        } else if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.ENDER_CHEST) {
            if (getConfig().getBoolean("anti-block-update.interact.chests", false))
                event.setCancelled(true);
        } else if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
            if (getConfig().getBoolean("anti-block-update.interact.furnace", false))
                event.setCancelled(true);
        } else if (type == Material.WORKBENCH) {
            if (getConfig().getBoolean("anti-block-update.interact.crafting-table", false))
                event.setCancelled(true);
        } else if (type == Material.ENCHANTMENT_TABLE) {
            if (getConfig().getBoolean("anti-block-update.interact.enchanting-table", false))
                event.setCancelled(true);
        } else if (type == Material.ANVIL) {
            if (getConfig().getBoolean("anti-block-update.interact.anvil", false))
                event.setCancelled(true);
        } else if (type == Material.BREWING_STAND) {
            if (getConfig().getBoolean("anti-block-update.interact.brewing-stand", false))
                event.setCancelled(true);
        } else if (type == Material.BEACON) {
            if (getConfig().getBoolean("anti-block-update.interact.beacon", false))
                event.setCancelled(true);
        } else if (type == Material.DISPENSER || type == Material.DROPPER) {
            if (getConfig().getBoolean("anti-block-update.interact.dispenser", false))
                event.setCancelled(true);
        } else if (type == Material.HOPPER) {
            if (getConfig().getBoolean("anti-block-update.interact.hopper", false))
                event.setCancelled(true);
        }
    }
}
