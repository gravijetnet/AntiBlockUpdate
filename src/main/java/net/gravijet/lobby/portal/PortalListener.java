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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {

    private final Plugin plugin;
    private final PortalManager portalManager;
    private final Map<UUID, String> playerInPortal = new HashMap<>();

    public PortalListener(Plugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
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
                portal.execute(player, plugin);
            }
        } else {
            playerInPortal.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerInPortal.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() != Material.BLAZE_ROD) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        event.setCancelled(true);
        int point = player.isSneaking() ? 2 : 1;
        portalManager.setSelection(player, event.getClickedBlock().getLocation(), point);
        String pointMsg = (point == 1) ? "§a§lfirst§f" : "§a§lsecond§f";
        player.sendMessage("§c§lGraviJet §7» §fSelection point " + pointMsg + " saved!");
    }
}
