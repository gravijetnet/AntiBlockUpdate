package net.gravijet.antiblockupdate;

import net.gravijet.lobby.portal.*;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private PortalManager portalManager;

    private static final Set<Material> PHYSICS_BLACKLIST = EnumSet.of(
            Material.SAND,
            Material.GRAVEL,
            Material.ANVIL,
            Material.DRAGON_EGG,
            Material.TORCH,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.WOOD_BUTTON,
            Material.TRIPWIRE_HOOK,
            Material.TRIPWIRE,
            Material.SIGN_POST,
            Material.WALL_SIGN,
            Material.LADDER,
            Material.VINE,
            Material.YELLOW_FLOWER,
            Material.RED_ROSE,
            Material.LONG_GRASS,
            Material.DEAD_BUSH,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.CACTUS,
            Material.SUGAR_CANE_BLOCK,
            Material.WHEAT,
            Material.CARROT,
            Material.POTATO,
            Material.NETHER_WARTS,
            Material.COCOA,
            Material.PUMPKIN_STEM,
            Material.MELON_STEM,
            Material.PUMPKIN,
            Material.MELON_BLOCK,
            Material.RAILS,
            Material.POWERED_RAIL,
            Material.DETECTOR_RAIL,
            Material.ACTIVATOR_RAIL,
            Material.WOODEN_DOOR,
            Material.IRON_DOOR_BLOCK,
            Material.TRAP_DOOR,
            Material.STONE_PLATE,
            Material.WOOD_PLATE,
            Material.IRON_PLATE,
            Material.GOLD_PLATE,
            Material.FENCE_GATE,
            Material.FIRE,
            Material.SNOW
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (getConfig().getBoolean("anti-block-update.physics", true)
                && PHYSICS_BLACKLIST.contains(event.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (getConfig().getBoolean("anti-block-update.falling-blocks", true)
                && event.getEntityType() == EntityType.FALLING_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (getConfig().getBoolean("anti-block-update.fire-spread", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (getConfig().getBoolean("anti-block-update.ice-snow-melt", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (getConfig().getBoolean("anti-block-update.plant-growth", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (getConfig().getBoolean("anti-block-update.liquid-flow", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (getConfig().getBoolean("anti-block-update.fire-ignition", true)) {
            BlockIgniteEvent.IgniteCause cause = event.getCause();
            if (cause == BlockIgniteEvent.IgniteCause.SPREAD || cause == BlockIgniteEvent.IgniteCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedInteract(PlayerInteractEvent event) {
        if (getConfig().getBoolean("anti-block-update.bed-interact", true)
                && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.BED_BLOCK) {
            event.setCancelled(true);
        }
    }
}
