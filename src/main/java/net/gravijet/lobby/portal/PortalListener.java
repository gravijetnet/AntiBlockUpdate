package net.gravijet.lobby.portal;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Listens for portal triggers and wand selection.
 */
public class PortalListener implements Listener {

    private final Plugin plugin;
    private final PortalManager portalManager;

    public PortalListener(Plugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    /**
     * Handle player movement to trigger portals.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            plugin.getLogger().fine("PlayerMoveEvent cancelled, but still checking for portals.");
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        // Only check if the block coordinates changed (performance)
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        Portal portalFrom = portalManager.getPortalAt(from);
        Portal portalTo = portalManager.getPortalAt(to);
        // Only trigger if player enters a portal (was outside, now inside)
        if (portalTo != null && (portalFrom == null || !portalFrom.equals(portalTo))) {
            plugin.getLogger().info("Portal triggered: " + portalTo.getName() + " by " + player.getName());
            portalTo.execute(player.getName());
        }
    }

    /**
     * Handle wand selection (Blaze Rod).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        // Check if holding a Blaze Rod
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return;
        }
        // Only allow right-clicking blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true); // Prevent block interaction with the wand

        int point = 1;
        if (player.isSneaking()) {
            point = 2;
        }
        portalManager.setSelection(player, event.getClickedBlock().getLocation(), point);

        String pointMsg = (point == 1) ? "§a§lfirst§f" : "§a§lsecond§f";
        player.sendMessage("§c§lGraviJet §7» §fSelection point " + pointMsg + " saved!");
    }

    /**
     * Bed blocker: cancel bed interaction.
     * This is already handled by existing BedInteractListener, but we can keep it here for completeness.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            // For Spigot 1.8.8, Material.BED_BLOCK is the correct one.
            if (event.getClickedBlock().getType() == Material.BED_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}