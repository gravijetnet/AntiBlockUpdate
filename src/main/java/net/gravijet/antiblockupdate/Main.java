package net.gravijet.antiblockupdate;

import net.gravijet.lobby.portal.*;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private PortalManager portalManager;
    private boolean portalEnabled;
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
    private boolean cfgIgnitionEnabled;
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

    private static Set<Material> safeMaterials(String... names) {
        Set<Material> set = new HashSet<>();
        for (String name : names) {
            try {
                set.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Material removed/renamed in this server version — skip silently
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private static final Set<Material> ALL_PHYSICS_BLOCKS = safeMaterials(
            "SAND", "GRAVEL", "ANVIL", "DRAGON_EGG",
            "TORCH", "LEVER",
            "STONE_BUTTON", "WOOD_BUTTON",
            "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON",
            "ACACIA_BUTTON", "DARK_OAK_BUTTON",
            "TRIPWIRE_HOOK", "TRIPWIRE",
            "SIGN_POST", "WALL_SIGN",
            "OAK_SIGN", "OAK_WALL_SIGN", "SPRUCE_SIGN", "SPRUCE_WALL_SIGN",
            "BIRCH_SIGN", "BIRCH_WALL_SIGN", "JUNGLE_SIGN", "JUNGLE_WALL_SIGN",
            "ACACIA_SIGN", "ACACIA_WALL_SIGN", "DARK_OAK_SIGN", "DARK_OAK_WALL_SIGN",
            "LADDER", "VINE",
            "YELLOW_FLOWER", "RED_ROSE",
            "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET",
            "RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY",
            "LONG_GRASS", "SHORT_GRASS", "TALL_GRASS",
            "DEAD_BUSH", "BROWN_MUSHROOM", "RED_MUSHROOM",
            "CACTUS",
            "SUGAR_CANE_BLOCK", "SUGAR_CANE",
            "WHEAT",
            "CARROT", "CARROTS",
            "POTATO", "POTATOES",
            "NETHER_WARTS", "NETHER_WART",
            "COCOA", "PUMPKIN_STEM", "MELON_STEM",
            "PUMPKIN",
            "MELON_BLOCK", "MELON",
            "RAILS", "RAIL", "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL",
            "WOODEN_DOOR", "IRON_DOOR_BLOCK", "TRAP_DOOR",
            "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR",
            "ACACIA_DOOR", "DARK_OAK_DOOR", "IRON_DOOR",
            "OAK_TRAPDOOR", "SPRUCE_TRAPDOOR", "BIRCH_TRAPDOOR",
            "JUNGLE_TRAPDOOR", "ACACIA_TRAPDOOR", "DARK_OAK_TRAPDOOR",
            "STONE_PLATE", "WOOD_PLATE", "IRON_PLATE", "GOLD_PLATE",
            "STONE_PRESSURE_PLATE", "OAK_PRESSURE_PLATE", "SPRUCE_PRESSURE_PLATE",
            "BIRCH_PRESSURE_PLATE", "JUNGLE_PRESSURE_PLATE", "ACACIA_PRESSURE_PLATE",
            "DARK_OAK_PRESSURE_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE",
            "HEAVY_WEIGHTED_PRESSURE_PLATE",
            "FENCE_GATE", "OAK_FENCE_GATE", "SPRUCE_FENCE_GATE", "BIRCH_FENCE_GATE",
            "JUNGLE_FENCE_GATE", "ACACIA_FENCE_GATE", "DARK_OAK_FENCE_GATE",
            "FIRE", "SNOW"
    );

    private static final Set<Material> ALL_PLANT_BLOCKS = safeMaterials(
            "WHEAT",
            "CARROT", "CARROTS",
            "POTATO", "POTATOES",
            "MELON_STEM", "PUMPKIN_STEM",
            "NETHER_WARTS", "NETHER_WART",
            "COCOA",
            "CACTUS",
            "SUGAR_CANE_BLOCK", "SUGAR_CANE"
    );

    private static final Set<Material> PRESSURE_PLATE_MATERIALS = safeMaterials(
            "STONE_PLATE", "WOOD_PLATE", "IRON_PLATE", "GOLD_PLATE",
            "STONE_PRESSURE_PLATE", "OAK_PRESSURE_PLATE", "SPRUCE_PRESSURE_PLATE",
            "BIRCH_PRESSURE_PLATE", "JUNGLE_PRESSURE_PLATE", "ACACIA_PRESSURE_PLATE",
            "DARK_OAK_PRESSURE_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE",
            "HEAVY_WEIGHTED_PRESSURE_PLATE"
    );

    // Fix #12: added IRON_DOOR (1.13+ name) which was missing
    private static final Set<Material> DOOR_MATERIALS = safeMaterials(
            "WOODEN_DOOR", "IRON_DOOR_BLOCK", "TRAP_DOOR",
            "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR",
            "ACACIA_DOOR", "DARK_OAK_DOOR", "IRON_DOOR",
            "OAK_TRAPDOOR", "SPRUCE_TRAPDOOR", "BIRCH_TRAPDOOR",
            "JUNGLE_TRAPDOOR", "ACACIA_TRAPDOOR", "DARK_OAK_TRAPDOOR",
            "FENCE_GATE", "OAK_FENCE_GATE", "SPRUCE_FENCE_GATE", "BIRCH_FENCE_GATE",
            "JUNGLE_FENCE_GATE", "ACACIA_FENCE_GATE", "DARK_OAK_FENCE_GATE"
    );

    private static final Set<Material> BUTTON_MATERIALS = safeMaterials(
            "STONE_BUTTON", "WOOD_BUTTON",
            "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON",
            "ACACIA_BUTTON", "DARK_OAK_BUTTON"
    );

    private static final Set<Material> BED_MATERIALS = safeMaterials(
            "BED_BLOCK",
            "WHITE_BED", "ORANGE_BED", "MAGENTA_BED", "LIGHT_BLUE_BED",
            "YELLOW_BED", "LIME_BED", "PINK_BED", "GRAY_BED",
            "LIGHT_GRAY_BED", "CYAN_BED", "PURPLE_BED", "BLUE_BED",
            "BROWN_BED", "GREEN_BED", "RED_BED", "BLACK_BED"
    );

    private static final Set<Material> CHEST_MATERIALS = safeMaterials(
            "CHEST", "TRAPPED_CHEST", "ENDER_CHEST"
    );

    private static final Set<Material> FURNACE_MATERIALS = safeMaterials(
            "FURNACE", "BURNING_FURNACE", "BLAST_FURNACE", "SMOKER"
    );

    // Fix #1: material names used as spread/fade switch cases changed in 1.13.
    // Resolve them at class-load time so they work on both 1.12 and 1.13+ servers.
    private static final Set<Material> GRASS_BLOCK_MATERIALS  = safeMaterials("GRASS", "GRASS_BLOCK");
    private static final Set<Material> MYCELIUM_MATERIALS     = safeMaterials("MYCEL", "MYCELIUM");
    private static final Set<Material> FARMLAND_MATERIALS     = safeMaterials("SOIL", "FARMLAND");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        buildSets();
        getServer().getPluginManager().registerEvents(this, this);

        // Fix #4: read portal.enabled after buildSets so config is already loaded
        portalEnabled = getConfig().getBoolean("portal.enabled", true);
        if (portalEnabled) {
            portalManager = new PortalManager(this);
            PortalListener portalListener = new PortalListener(this, portalManager);
            portalManager.setPortalListener(portalListener);
            getServer().getPluginManager().registerEvents(portalListener, this);
            PortalCommand portalCmd = new PortalCommand(portalManager);
            if (getCommand("portal") != null) {
                getCommand("portal").setExecutor(portalCmd);
                getCommand("portal").setTabCompleter(portalCmd);
            }
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        getLogger().info("AntiBlockUpdate enabled.");
    }

    @Override
    public void onDisable() {
        if (portalEnabled) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        }
        getLogger().info("AntiBlockUpdate disabled.");
    }

    private void buildSets() {
        FileConfiguration c = getConfig();

        cfgPhysicsEnabled = c.getBoolean("anti-block-update.physics.enabled", true);
        physicsBlacklist = new HashSet<>();
        for (Material mat : ALL_PHYSICS_BLOCKS) {
            if (c.getBoolean("anti-block-update.physics.blocks." + mat.name(), true))
                physicsBlacklist.add(mat);
        }

        cfgPlantGrowthEnabled = c.getBoolean("anti-block-update.plant-growth.enabled", true);
        plantBlacklist = new HashSet<>();
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
        cfgIgnitionEnabled = c.getBoolean("anti-block-update.fire.ignition.enabled", true);
        cfgBlockedIgniteCauses = EnumSet.noneOf(BlockIgniteEvent.IgniteCause.class);
        if (cfgIgnitionEnabled) {
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

        cfgTntExplosion         = c.getBoolean("anti-block-update.explosions.tnt", false);
        cfgCreeperExplosion     = c.getBoolean("anti-block-update.explosions.creeper", false);
        cfgWitherExplosion      = c.getBoolean("anti-block-update.explosions.wither", false);
        cfgFireballExplosion    = c.getBoolean("anti-block-update.explosions.fireball", false);
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

        cfgItemDrop        = c.getBoolean("anti-block-update.item-drop", false);
        cfgItemPickup      = c.getBoolean("anti-block-update.item-pickup", false);
        cfgHungerDepletion = c.getBoolean("anti-block-update.hunger-depletion", false);
        cfgWeatherChange   = c.getBoolean("anti-block-update.weather-change", false);

        cfgInteractBed             = c.getBoolean("anti-block-update.interact.bed", true);
        cfgInteractDoors           = c.getBoolean("anti-block-update.interact.doors", false);
        cfgInteractButtons         = c.getBoolean("anti-block-update.interact.buttons", false);
        cfgInteractLever           = c.getBoolean("anti-block-update.interact.lever", false);
        cfgInteractPressurePlates  = c.getBoolean("anti-block-update.interact.pressure-plates", false);
        cfgInteractNoteBlock       = c.getBoolean("anti-block-update.interact.note-block", false);
        cfgInteractJukebox         = c.getBoolean("anti-block-update.interact.jukebox", false);
        cfgInteractChests          = c.getBoolean("anti-block-update.interact.chests", false);
        cfgInteractFurnace         = c.getBoolean("anti-block-update.interact.furnace", false);
        cfgInteractCraftingTable   = c.getBoolean("anti-block-update.interact.crafting-table", false);
        cfgInteractEnchantingTable = c.getBoolean("anti-block-update.interact.enchanting-table", false);
        cfgInteractAnvil           = c.getBoolean("anti-block-update.interact.anvil", false);
        cfgInteractBrewingStand    = c.getBoolean("anti-block-update.interact.brewing-stand", false);
        cfgInteractBeacon          = c.getBoolean("anti-block-update.interact.beacon", false);
        cfgInteractDispenser       = c.getBoolean("anti-block-update.interact.dispenser", false);
        cfgInteractHopper          = c.getBoolean("anti-block-update.interact.hopper", false);
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
        if (!cfgIgnitionEnabled) return;
        if (cfgBlockedIgniteCauses.contains(event.getCause()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK SPREAD — fire, grass, mycelium, mushroom, vine
    // Fix #1/#2: GRASS→GRASS_BLOCK and MYCEL→MYCELIUM in 1.13; use set lookup
    // instead of switch-case enum literals that don't exist on newer servers.
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Material newType = event.getNewState().getType();
        String name = newType.name();
        if (name.equals("FIRE")) {
            if (cfgFireSpread) event.setCancelled(true);
        } else if (GRASS_BLOCK_MATERIALS.contains(newType)) {
            if (cfgSpreadGrass) event.setCancelled(true);
        } else if (MYCELIUM_MATERIALS.contains(newType)) {
            if (cfgSpreadMycelium) event.setCancelled(true);
        } else if (name.equals("BROWN_MUSHROOM") || name.equals("RED_MUSHROOM")) {
            if (cfgSpreadMushroom) event.setCancelled(true);
        } else if (name.equals("VINE")) {
            if (cfgSpreadVine) event.setCancelled(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCK FADE — ice melt, snow melt, fire extinguish, farmland dry
    // Fix #3: SOIL→FARMLAND in 1.13; use set lookup instead of switch-case.
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        String name = type.name();
        if (name.equals("ICE")) {
            if (cfgIceMelt) event.setCancelled(true);
        } else if (name.equals("SNOW")) {
            if (cfgSnowMelt) event.setCancelled(true);
        } else if (name.equals("FIRE")) {
            if (cfgFireExtinguish) event.setCancelled(true);
        } else if (FARMLAND_MATERIALS.contains(type)) {
            if (cfgFarmlandDry) event.setCancelled(true);
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
        if (cfgPlantGrowthEnabled && plantBlacklist.contains(event.getNewState().getType()))
            event.setCancelled(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LIQUID FLOW + DRAGON EGG TELEPORT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        String typeName = type.name();
        if ((typeName.equals("WATER") || typeName.equals("STATIONARY_WATER")) && cfgWaterFlow) {
            event.setCancelled(true);
        } else if ((typeName.equals("LAVA") || typeName.equals("STATIONARY_LAVA")) && cfgLavaFlow) {
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
        org.bukkit.entity.Entity rawDamager = event.getDamager();
        // Fix #7: when the projectile shooter is a non-entity source (e.g. dispenser),
        // keep rawDamager so the checks below simply fall through rather than treating
        // a block dispenser as if it were a player or monster.
        org.bukkit.entity.Entity attacker = rawDamager;
        if (rawDamager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) rawDamager).getShooter();
            if (shooter instanceof org.bukkit.entity.Entity) {
                attacker = (org.bukkit.entity.Entity) shooter;
            }
        }

        if (attacker instanceof Player) {
            if (event.getEntity() instanceof Player) {
                if (cfgBlockPvp) event.setCancelled(true);
            } else if (event.getEntity() instanceof Monster) {
                if (cfgBlockHitMonsters) event.setCancelled(true);
            } else {
                if (cfgBlockHitMobs) event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Player && attacker instanceof Monster) {
            if (cfgBlockMonsterDamage) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!cfgBlockFallDamage && !cfgBlockVoidDamage) return;
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
        // Only cancel the transition to stormy weather; clearing is left untouched
        // so the world doesn't get stuck in permanent storm if the plugin reloads mid-storm.
        if (cfgWeatherChange && event.toWeatherState()) event.setCancelled(true);
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
            if (cfgInteractPressurePlates && PRESSURE_PLATE_MATERIALS.contains(type))
                event.setCancelled(true);
            return;
        }

        if (action != Action.RIGHT_CLICK_BLOCK) return;

        if (BED_MATERIALS.contains(type)) {
            if (cfgInteractBed) event.setCancelled(true);
        } else if (DOOR_MATERIALS.contains(type)) {
            if (cfgInteractDoors) event.setCancelled(true);
        } else if (BUTTON_MATERIALS.contains(type)) {
            if (cfgInteractButtons) event.setCancelled(true);
        } else if (type == Material.LEVER) {
            if (cfgInteractLever) event.setCancelled(true);
        } else if (PRESSURE_PLATE_MATERIALS.contains(type)) {
            if (cfgInteractPressurePlates) event.setCancelled(true);
        } else if (type == Material.NOTE_BLOCK) {
            if (cfgInteractNoteBlock) event.setCancelled(true);
        } else if (type == Material.JUKEBOX) {
            if (cfgInteractJukebox) event.setCancelled(true);
        } else if (CHEST_MATERIALS.contains(type)) {
            if (cfgInteractChests) event.setCancelled(true);
        } else if (FURNACE_MATERIALS.contains(type)) {
            if (cfgInteractFurnace) event.setCancelled(true);
        } else if (isCraftingTable(type)) {
            if (cfgInteractCraftingTable) event.setCancelled(true);
        } else if (isEnchantingTable(type)) {
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

    // ── Material compatibility helpers (1.12 vs 1.13+ name changes) ──────────

    private static boolean isCraftingTable(Material m) {
        String n = m.name();
        return n.equals("WORKBENCH") || n.equals("CRAFTING_TABLE");
    }

    private static boolean isEnchantingTable(Material m) {
        String n = m.name();
        return n.equals("ENCHANTMENT_TABLE") || n.equals("ENCHANTING_TABLE");
    }
}
