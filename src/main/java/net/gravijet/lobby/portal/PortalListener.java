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

    private static final String PERM_USE    = "antiblockupdate.portal.use";
    private static final String PERM_MANAGE = "antiblockupdate.portal.manage";

    private final Plugin plugin;
    private final PortalManager portalManager;
    // Tracks which portal name each player is currently standing in to avoid re-firing.
    private final Map<UUID, String> playerInPortal = new ConcurrentHashMap<>();

    public PortalListener(Plugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    // Fix #26: ignoreCancelled = true so cancelled move events (e.g. from other plugins)
    // don't trigger portal logic; also avoids the per-packet overhead on cancelled events.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        // Skip head-only rotation — block coords unchanged means no portal crossing.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Portal portal = portalManager.getPortalAt(to);

        if (portal != null) {
            // Use compute() for an atomic check-and-set: the lambda runs under the map's
            // internal lock for this key, so only one thread can win the "new portal" race.
            final String[] shouldExecute = {null};
            playerInPortal.compute(uuid, (k, current) -> {
                if (current == null || !current.equals(portal.getName())) {
                    shouldExecute[0] = portal.getName();
                    return portal.getName();
                }
                return current;
            });
            if (shouldExecute[0] != null) {
                boolean success = portal.execute(player, plugin);
                if (!success) {
                    playerInPortal.remove(uuid, portal.getName());
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
     * Without this, a player standing in a reloaded portal would never re-trigger it.
     */
    public void clearAllPortalState() {
        playerInPortal.clear();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();
        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        if (!player.hasPermission(PERM_MANAGE)) return;

        event.setCancelled(true);
        int point = player.isSneaking() ? 2 : 1;

        // Fix #30: if point 1 has never been set, getSelectionWorld() returns null and
        // the cross-world guard is skipped — which is correct (no existing world to compare).
        // For point 2, only enforce world consistency when point 1 is already set.
        if (point == 2) {
            String existingWorld = portalManager.getSelectionWorld(player);
            org.bukkit.World clickedWorld = event.getClickedBlock().getWorld();
            if (clickedWorld == null) {
                player.sendMessage("§cCould not determine the world of the clicked block.");
                return;
            }
            if (existingWorld != null && !existingWorld.equals(clickedWorld.getName())) {
                player.sendMessage("§cBoth selection points must be in the same world. Point 2 not saved.");
                return;
            }
        }

        portalManager.setSelection(player, event.getClickedBlock().getLocation(), point);
        String pointMsg = (point == 1) ? "§a§lfirst§f" : "§a§lsecond§f";
        player.sendMessage("§c§lGraviJet §7» §fSelection point " + pointMsg + " saved!");
    }
}
