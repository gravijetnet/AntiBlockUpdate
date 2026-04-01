package net.gravijet.antiblockupdate.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BedInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            // Check for both BED_BLOCK and BED, as BED_BLOCK is for older versions and BED for newer.
            // For Spigot 1.8.8, Material.BED_BLOCK is the correct one.
            if (event.getClickedBlock().getType() == Material.BED_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}
