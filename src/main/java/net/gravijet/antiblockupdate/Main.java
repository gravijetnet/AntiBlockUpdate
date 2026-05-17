package net.gravijet.antiblockupdate;

import net.gravijet.lobby.portal.*;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
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

    // ── Cached config values (populated by buildSets on enable/reload) ──────
    private boolean cfgPhysicsEnabled;
    private boolean cfgFallingBlocks;
    private boolean cfgEndermanBlock;
    private boolean cfgSheepEatGrass;
    private boolean cfgSilverfishBlock;
    private boolean cfgWitherBlock;
    private boolean cfgZombieBreakDoor;
    private boolean cfgFireBurn;
    private Set<BlockIgniteEvent.IgniteCause> cfgBlockedIgniteCauses;
    private boolean cfgFireSpread;
    private boolean cfgSpreadGrass;
    private boolean cfgSpreadMycelium;
    private boolean cfgSpreadMushroom;
    private boolean cfgSpreadVine;
    private boolean cfgIceMelt;
    private boolean cfgSnowMelt;
    private boolean cfgFireExtinguish;
    private boolean cfgFarmlandDry;
    private boolean cfgSnowForm;
    private boolean cfgIceForm;
    private boolean cfgObsidianForm;
    private boolean cfgCobblestoneForm;
    private boolean cfgPlantGrowthEnabled;
    private boolean cfgWaterFlow;
    private boolean cfgLavaFlow;
    private boolean cfgDragonEggTeleport;
    private boolean cfgLeavesDecay;
    private boolean cfgGiantMushroomGrow;
    private boolean cfgTreeGrow;
    private boolean cfgTntExplosion;
    private boolean cfgCreeperExplosion;
    private boolean cfgWitherExplosion;
    private boolean cfgFireballExplosion;
    private boolean cfgWitherSkullExplosion;
    private boolean cfgPistonExtend;
    private boolean cfgPistonRetract;
    private boolean cfgRedstoneChange;
    private boolean cfgDispenserDispense;
    private boolean cfgBlockBreak;
    private boolean cfgBlockPlace;
    private boolean cfgBlockDamage;
    private boolean cfgBucketFill;
    private boolean cfgBucketEmpty;
    private boolean cfgNaturalMonsters;
    private boolean cfgNaturalMobs;
    private boolean cfgBlockPvp;
    private boolean cfgBlockHitMonsters;
    private boolean cfgBlockHitMobs;
    private boolean cfgBlockMonsterDamage;
    private boolean cfgBlockFallDamage;
    private boolean cfgBlockVoidDamage;
    private boolean cfgItemDrop;
    private boolean cfgItemPickup;
    private boolean cfgHungerDepletion;
    private boolean cfgWeatherChange;
    private boolean cfgInteractBed;
    private boolean cfgInteractDoors;
    private boolean cfgInteractButtons;
    private boolean cfgInteractLever;
    private boolean cfgInteractPressurePlates;
    private boolean cfgInteractNoteBlock;
    private boolean cfgInteractJukebox;
    private boolean cfgInteractChests;
    private boolean cfgInteractFurnace;
    private boolean cfgInteractCraftingTable;
    private boolean cfgInteractEnchantingTable;
    private boolean cfgInteractAnvil;
    private boolean cfgInteractBrewingStand;
    private boolean cfgInteractBeacon;
    private boolean cfgInteractDispenser;
    private boolean cfgInteractHopper;

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
        FileConfiguration c = getConfig();

        cfgPhysicsEnabled = c.getBoolean("anti-block-update.physics.enabled", true);
        physicsBlacklist = EnumSet.noneOf(Material.class);
        for (Material mat : ALL_PHYSICS_BLOCKS) {
            if (c.getBoolean("anti-block-update.physics.blocks." + mat.name(), true))
                physicsBlacklist.add(mat);
        }

        cfgPlantGrowthEnabled = c.getBoolean("anti-block-update.plant-growth.enabled", true);
        plantBlacklist = EnumSet.noneOf(Material.class);
        for (Material mat : ALL_PLANT_BLOCKS) {
            if (c.getBoolean("anti-block-update.plant-growth.plants." + mat.name(), true))
                plantBlacklist.add(mat);
        }

        cfgFallingBlocks    = c.getBoolean("anti-block-update.falling-blocks", true);
        cfgEndermanBlock    = c.getBoolean("anti-block-update.entity-block.enderman", true);
        cfgSheepEatGrass    = c.getBoolean("anti-block-update.entity-block.sheep-eat-grass", true);
        cfgSilverfishBlock  = c.getBoolean("anti-block-update.entity-block.silverfish", true);
        cfgWitherBlock      = c.getBoolean("anti-block-update.entity-block.wither", true);
        cfgZombieBreakDoor  = c.getBoolean("anti-block-update.entity-block.zombie-break-door", false);

        cfgFireBurn       = c.getBoolean("anti-block-update.fire.burn", true);
        cfgFireSpread     = c.getBoolean("anti-block-update.fire.spread", true);
        cfgFireExtinguish = c.getBoolean("anti-block-update.fire.extinguish", false);
        cfgBlockedIgniteCauses = EnumSet.noneOf(BlockIgniteEvent.IgniteCause.class);
        if (c.getBoolean("anti-block-update.fire.ignition.enabled", true)) {
            for (BlockIgniteEvent.IgniteCause cause : BlockIgniteEvent.IgniteCause.values()) {
                if (c.getBoolean("anti-block-update.fire.ignition.causes." + cause.name(), false))
                    cfgBlockedIgniteCauses.add(cause);
            }
        }

        cfgSpreadGrass    = c.getBoolean("anti-block-update.spread.grass", true);
        cfgSpreadMycelium = c.getBoolean("anti-block-update.spread.mycelium", true);
        cfgSpreadMushroom = c.getBoolean("anti-block-update.spread.mushroom", true);
        cfgSpreadVine     = c.getBoolean("anti-block-update.spread.vine", true);

        cfgIceMelt   = c.getBoolean("anti-block-update.ice.melt", true);
        cfgIceForm   = c.getBoolean("anti-block-update.ice.form", false);
        cfgSnowMelt  = c.getBoolean("anti-block-update.snow.melt", true);
        cfgSnowForm  = c.getBoolean("anti-block-update.snow.form", false);
        cfgFarmlandDry = c.getBoolean("anti-block-update.farmland-dry", false);

        cfgObsidianForm    = c.getBoolean("anti-block-update.block-form.obsidian", false);
        cfgCobblestoneForm = c.getBoolean("anti-block-update.block-form.cobblestone", false);

        cfgWaterFlow         = c.getBoolean("anti-block-update.liquid-flow.water", false);
        cfgLavaFlow          = c.getBoolean("anti-block-update.liquid-flow.lava", false);
        cfgDragonEggTeleport = c.getBoolean("anti-block-update.dragon-egg-teleport", true);
        cfgLeavesDecay       = c.getBoolean("anti-block-update.leaves-decay", true);

        cfgGiantMushroomGrow = c.getBoolean("anti-block-update.structure-grow.giant-mushroom", false);
        cfgTreeGrow          = c.getBoolean("anti-block-update.structure-grow.tree", false);

        cfgTntExplosion       = c.getBoolean("anti-block-update.explosions.tnt", false);
        cfgCreeperExplosion   = c.getBoolean("anti-block-update.explosions.creeper", false);
        cfgWitherExplosion    = c.getBoolean("anti-block-update.explosions.wither", false);
        cfgFireballExplosion  = c.getBoolean("anti-block-update.explosions.fireball", false);
        cfgWitherSkullExplosion = c.getBoolean("anti-block-update.explosions.wither-skull", false);

        cfgPistonExtend    = c.getBoolean("anti-block-update.pistons.extend", false);
        cfgPistonRetract   = c.getBoolean("anti-block-update.pistons.retract", false);
        cfgRedstoneChange  = c.getBoolean("anti-block-update.redstone-change", false);
        cfgDispenserDispense = c.getBoolean("anti-block-update.dispenser-dispense", false);

        cfgBlockBreak  = c.getBoolean("anti-block-update.block-break", false);
        cfgBlockPlace  = c.getBoolean("anti-block-update.block-place", false);
        cfgBlockDamage = c.getBoolean("anti-block-update.block-damage", false);

        cfgBucketFill  = c.getBoolean("anti-block-update.buckets.fill", false);
        cfgBucketEmpty = c.getBoolean("anti-block-update.buckets.empty", false);

        cfgNaturalMonsters = c.getBoolean("anti-block-update.mob-spawn.natural-monsters", false);
        cfgNaturalMobs     = c.getBoolean("anti-block-update.mob-spawn.natural-mobs", false);

        cfgBlockPvp           = c.getBoolean("anti-block-update.combat.block-pvp", false);
        cfgBlockHitMonsters   = c.getBoolean("anti-block-update.combat.block-hit-monsters", false);
        cfgBlockHitMobs       = c.getBoolean("anti-block-update.combat.block-hit-mobs", false);
        cfgBlockMonsterDamage = c.getBoolean("anti-block-update.combat.block-monster-damage", false);
        cfgBlockFallDamage    = c.getBoolean("anti-block-update.combat.block-fall-damage", false);
        cfgBlockVoidDamage    = c.getBoolean("anti-block-update.combat.block-void-damage", false);

        cfgItemDrop       = c.getBoolean("anti-block-update.item-drop", false);
        cfgItemPickup     = c.getBoolean("anti-block-update.item-pickup", false);
        cfgHungerDepletion = c.getBoolean("anti-block-update.hunger-depletion", false);
        cfgWeatherChange   = c.getBoolean("anti-block-update.weather-change", false);

        cfgInteractBed            = c.getBoolean("anti-block-update.interact.bed", true);
        cfgInteractDoors          = c.getBoolean("anti-block-update.interact.doors", false);
        cfgInteractButtons        = c.getBoolean("anti-block-update.interact.buttons", false);
        cfgInteractLever          = c.getBoolean("anti-block-update.interact.lever", false);
        cfgInteractPressurePlates = c.getBoolean("anti-block-update.interact.pressure-plates", false);
        cfgInteractNoteBlock      = c.getBoolean("anti-block-update.interact.note-block", false);
        cfgInteractJukebox        = c.getBoolean("anti-block-update.interact.jukebox", false);
        cfgInteractChests         = c.getBoolean("anti-block-update.interact.chests", false);
        cfgInteractFurnace        = c.getBoolean("anti-block-update.interact.furnace", false);
        cfgInteractCraftingTable  = c.getBoolean("anti-block-update.interact.crafting-table", false);
        cfgInteractEnchantingTable = c.getBoolean("anti-block-update.interact.enchanting-table", false);
        cfgInteractAnvil          = c.getBoolean("anti-block-update.interact.anvil", false);
        cfgInteractBrewingStand   = c.getBoolean("anti-block-update.interact.brewing-stand", false);
        cfgInteractBeacon         = c.getBoolean("anti-block-update.interact.beacon", false);
        cfgInteractDispenser      = c.getBoolean("anti-block-update.interact.dispenser", false);
        cfgInteractHopper         = c.getBoolean("anti-block-update.interact.hopper", false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PHYSICS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (cfgPhysicsEnabled && physicsBlacklist.contains(event.getBlock().getType()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTITY BLOCK CHANGES
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        switch (event.getEntityType()) {
            case FALLING_BLOCK:
                if (cfgFallingBlocks) event.setCancelled(true);
                break;
            case ENDERMAN:
                if (cfgEndermanBlock) event.setCancelled(true);
                break;
            case SHEEP:
                if (cfgSheepEatGrass) event.setCancelled(true);
                break;
            case SILVERFISH:
                if (cfgSilverfishBlock) event.setCancelled(true);
                break;
            case WITHER:
                if (cfgWitherBlock) event.setCancelled(true);
                break;
            case ZOMBIE:
                if (cfgZombieBreakDoor) event.setCancelled(true);
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
        if (cfgFireBurn) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (cfgBlockedIgniteCauses.contains(event.getCause()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK SPREAD — fire, grass, mycelium, mushroom, vine
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        switch (event.getNewState().getType()) {
            case FIRE:
                if (cfgFireSpread) event.setCancelled(true);
                break;
            case GRASS:
                if (cfgSpreadGrass) event.setCancelled(true);
                break;
            case MYCEL:
                if (cfgSpreadMycelium) event.setCancelled(true);
                break;
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
                if (cfgSpreadMushroom) event.setCancelled(true);
                break;
            case VINE:
                if (cfgSpreadVine) event.setCancelled(true);
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
                if (cfgIceMelt) event.setCancelled(true);
                break;
            case SNOW:
                if (cfgSnowMelt) event.setCancelled(true);
                break;
            case FIRE:
                if (cfgFireExtinguish) event.setCancelled(true);
                break;
            case SOIL:
                if (cfgFarmlandDry) event.setCancelled(true);
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
                if (cfgSnowForm) event.setCancelled(true);
                break;
            case ICE:
                if (cfgIceForm) event.setCancelled(true);
                break;
            case OBSIDIAN:
                if (cfgObsidianForm) event.setCancelled(true);
                break;
            case COBBLESTONE:
                if (cfgCobblestoneForm) event.setCancelled(true);
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
        if (cfgPlantGrowthEnabled && plantBlacklist.contains(event.getBlock().getType()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LIQUID FLOW + DRAGON EGG TELEPORT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        if ((type == Material.WATER || type == Material.STATIONARY_WATER) && cfgWaterFlow) {
            event.setCancelled(true);
        } else if ((type == Material.LAVA || type == Material.STATIONARY_LAVA) && cfgLavaFlow) {
            event.setCancelled(true);
        } else if (type == Material.DRAGON_EGG && cfgDragonEggTeleport) {
            event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LEAVES DECAY
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (cfgLeavesDecay) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STRUCTURE GROWTH — trees & giant mushrooms
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        TreeType species = event.getSpecies();
        if (species == TreeType.RED_MUSHROOM || species == TreeType.BROWN_MUSHROOM) {
            if (cfgGiantMushroomGrow) event.setCancelled(true);
        } else {
            if (cfgTreeGrow) event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXPLOSIONS — block damage only (entity damage is unaffected)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        switch (event.getEntityType()) {
            case PRIMED_TNT:
                if (cfgTntExplosion) event.blockList().clear();
                break;
            case CREEPER:
                if (cfgCreeperExplosion) event.blockList().clear();
                break;
            case WITHER:
                if (cfgWitherExplosion) event.blockList().clear();
                break;
            case FIREBALL:
            case SMALL_FIREBALL:
                if (cfgFireballExplosion) event.blockList().clear();
                break;
            case WITHER_SKULL:
                if (cfgWitherSkullExplosion) event.blockList().clear();
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
        if (cfgPistonExtend) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (cfgPistonRetract) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REDSTONE  (not Cancellable — reset current to freeze it)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (cfgRedstoneChange) event.setNewCurrent(event.getOldCurrent());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DISPENSERS & DROPPERS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (cfgDispenserDispense) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK BREAK / PLACE / DAMAGE
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (cfgBlockBreak) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (cfgBlockPlace) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (cfgBlockDamage) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUCKETS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (cfgBucketFill) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (cfgBucketEmpty) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MOB SPAWNING
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (event.getEntity() instanceof Monster) {
            if (cfgNaturalMonsters) event.setCancelled(true);
        } else {
            if (cfgNaturalMobs) event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMBAT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof Player) {
                if (cfgBlockPvp) event.setCancelled(true);
            } else if (event.getEntity() instanceof Monster) {
                if (cfgBlockHitMonsters) event.setCancelled(true);
            } else {
                if (cfgBlockHitMobs) event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Player && event.getDamager() instanceof Monster) {
            if (cfgBlockMonsterDamage) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && cfgBlockFallDamage)
            event.setCancelled(true);
        else if (event.getCause() == EntityDamageEvent.DamageCause.VOID && cfgBlockVoidDamage)
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ITEM HANDLING
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (cfgItemDrop) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (cfgItemPickup) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HUNGER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (cfgHungerDepletion && event.getFoodLevel() < player.getFoodLevel())
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WEATHER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (cfgWeatherChange) event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PLAYER INTERACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Action action = event.getAction();
        Material type = event.getClickedBlock().getType();

        // Walking on pressure plates fires PHYSICAL, not RIGHT_CLICK_BLOCK
        if (action == Action.PHYSICAL) {
            if (cfgInteractPressurePlates
                    && (type == Material.STONE_PLATE || type == Material.WOOD_PLATE
                    || type == Material.IRON_PLATE || type == Material.GOLD_PLATE))
                event.setCancelled(true);
            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK) return;

        if (type == Material.BED_BLOCK) {
            if (cfgInteractBed) event.setCancelled(true);
        } else if (type == Material.WOODEN_DOOR || type == Material.TRAP_DOOR || type == Material.FENCE_GATE) {
            if (cfgInteractDoors) event.setCancelled(true);
        } else if (type == Material.STONE_BUTTON || type == Material.WOOD_BUTTON) {
            if (cfgInteractButtons) event.setCancelled(true);
        } else if (type == Material.LEVER) {
            if (cfgInteractLever) event.setCancelled(true);
        } else if (type == Material.STONE_PLATE || type == Material.WOOD_PLATE
                || type == Material.IRON_PLATE || type == Material.GOLD_PLATE) {
            if (cfgInteractPressurePlates) event.setCancelled(true);
        } else if (type == Material.NOTE_BLOCK) {
            if (cfgInteractNoteBlock) event.setCancelled(true);
        } else if (type == Material.JUKEBOX) {
            if (cfgInteractJukebox) event.setCancelled(true);
        } else if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.ENDER_CHEST) {
            if (cfgInteractChests) event.setCancelled(true);
        } else if (type == Material.FURNACE || type == Material.BURNING_FURNACE) {
            if (cfgInteractFurnace) event.setCancelled(true);
        } else if (type == Material.WORKBENCH) {
            if (cfgInteractCraftingTable) event.setCancelled(true);
        } else if (type == Material.ENCHANTMENT_TABLE) {
            if (cfgInteractEnchantingTable) event.setCancelled(true);
        } else if (type == Material.ANVIL) {
            if (cfgInteractAnvil) event.setCancelled(true);
        } else if (type == Material.BREWING_STAND) {
            if (cfgInteractBrewingStand) event.setCancelled(true);
        } else if (type == Material.BEACON) {
            if (cfgInteractBeacon) event.setCancelled(true);
        } else if (type == Material.DISPENSER || type == Material.DROPPER) {
            if (cfgInteractDispenser) event.setCancelled(true);
        } else if (type == Material.HOPPER) {
            if (cfgInteractHopper) event.setCancelled(true);
        }
    }
}
