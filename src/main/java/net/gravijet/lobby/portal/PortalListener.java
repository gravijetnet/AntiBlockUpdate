package net.gravijet.lobby.portal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PortalListener implements Listener {

    private final Plugin plugin;
    private final PortalManager portalManager;
    private final Map<UUID, String> playerInPortal = new ConcurrentHashMap<>();

    public PortalListener(Plugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        // BUG-20 (was BUG-18 in old report): to can be null on some Bukkit versions
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Portal portal = portalManager.getPortalAt(to);

        if (portal != null) {
            String current = playerInPortal.get(uuid);
            if (!portal.getName().equals(current)) {
                playerInPortal.put(uuid, portal.getName());
                // BUG-18: if execute() fails, remove the entry so the next block-crossing retries
                boolean success = portal.execute(player, plugin);
                if (!success) {
                    playerInPortal.remove(uuid);
                }
            }
        } else {
            playerInPortal.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerInPortal.remove(player.getUniqueId());
        portalManager.clearSelection(player);
    }

    /**
     * Called by PortalManager.reloadPortals() so stale playerInPortal entries are cleared.
     * BUG-17: without this, a player standing in a reloaded portal would never re-trigger it.
     */
    public void clearAllPortalState() {
        playerInPortal.clear();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // BUG-20: getItemInMainHand() added in 1.9; returns AIR (never null) on 1.9+
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BLAZE_ROD) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        event.setCancelled(true);
        int point = player.isSneaking() ? 2 : 1;
        if (point == 2) {
            String existingWorld = portalManager.getSelectionWorld(player);
            if (existingWorld != null && !existingWorld.equals(event.getClickedBlock().getWorld().getName())) {
                // BUG-19: warn and refuse the cross-world point rather than saving it silently
                player.sendMessage("§cBoth selection points must be in the same world. Point 2 not saved.");
                return;
            }
        }
        portalManager.setSelection(player, event.getClickedBlock().getLocation(), point);
        String pointMsg = (point == 1) ? "§a§lfirst§f" : "§a§lsecond§f";
        player.sendMessage("§c§lGraviJet §7» §fSelection point " + pointMsg + " saved!");
    }
}
