package net.gravijet.antiblockupdate;

import net.gravijet.antiblockupdate.listener.BedInteractListener;
import net.gravijet.lobby.portal.*;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private PortalManager portalManager;

    /**
     * Fallende Entitäten, die durch EntityChangeBlockEvent abgedeckt werden.
     * Sand, Kies, Drachenej, Amboss usw.
     */
    private static final Set<EntityType> FALLING_ENTITIES = EnumSet.of(
            EntityType.FALLING_BLOCK
    );

    /**
     * Blöcke, die durch BlockPhysicsEvent NICHT weitergeleitet werden sollen.
     * Enthält alle Materialien, die auf Schwerkraft oder Nachbar-Updates reagieren.
     */
    private static final Set<Material> PHYSICS_BLACKLIST = EnumSet.of(
            // Schwerkraft-Blöcke
            Material.SAND,
            Material.GRAVEL,
            Material.ANVIL,
            Material.DRAGON_EGG,
            // Flüssigkeiten (sicherheitshalber – primär via BlockFromToEvent)
            Material.WATER,
            Material.LAVA,
            Material.STATIONARY_WATER,
            Material.STATIONARY_LAVA,
            // Hängende / stützpunktabhängige Blöcke
            Material.TORCH,
            Material.REDSTONE_TORCH_OFF,
            Material.REDSTONE_TORCH_ON,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.WOOD_BUTTON,
            Material.TRIPWIRE_HOOK,
            Material.TRIPWIRE,
            Material.SIGN_POST,
            Material.WALL_SIGN,
            Material.LADDER,
            Material.VINE,
            // Pflanzen / Blumen
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
            // Kaktus, Kürbis, Melone
            Material.PUMPKIN,
            Material.MELON_BLOCK,
            // Hängende Blöcke
            Material.RAILS,
            Material.POWERED_RAIL,
            Material.DETECTOR_RAIL,
            Material.ACTIVATOR_RAIL,
            // Türen / Falltüren
            Material.WOODEN_DOOR,
            Material.IRON_DOOR_BLOCK,
            Material.TRAP_DOOR,
            // Druckplatten
            Material.STONE_PLATE,
            Material.WOOD_PLATE,
            Material.IRON_PLATE,
            Material.GOLD_PLATE,
            // Zaun- / Tor-Verbinder (Update bei Nachbarn)
            Material.FENCE_GATE,
            // Sonstige physik-abhängige
            Material.FIRE,
            Material.SNOW,
            Material.DIODE_BLOCK_OFF,
            Material.DIODE_BLOCK_ON,
            Material.REDSTONE_COMPARATOR_OFF,
            Material.REDSTONE_COMPARATOR_ON,
            Material.REDSTONE_WIRE
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new BedInteractListener(), this);

        // Initialize Portal System
        portalManager = new PortalManager(this);
        getServer().getPluginManager().registerEvents(new PortalListener(this, portalManager), this);
        getCommand("portal").setExecutor(new PortalCommand(portalManager));

        getLogger().info("AntiBlockUpdate aktiviert – alle Block-Updates deaktiviert.");
        getLogger().info("Portal system loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiBlockUpdate deaktiviert.");
    }

    // -------------------------------------------------------------------------
    // 1) Physikalische Block-Updates (Sand-in-der-Luft, Blume-ohne-Untergrund…)
    // -------------------------------------------------------------------------

    /**
     * Blockiert ALLE physikalischen Block-Updates.
     *
     * HIGHEST + ignoreCancelled=true: Wir greifen als Letzter ein,
     * sodass andere Plugins zuerst reagieren können; danach canceln wir hart.
     *
     * Durch das pauschale Canceln bleibt Sand in der Luft, bleiben Blumen
     * an nicht unterstützten Positionen erhalten und reagieren Blöcke nicht
     * auf das Platzieren/Entfernen von Nachbarblöcken.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // 2) Sand / Kies Schwerkraft (Entity-basiert)
    // -------------------------------------------------------------------------

    /**
     * Verhindert, dass Sand/Kies-Entitäten entstehen.
     * Ohne diesen Handler würde Schwerkraft erst durch BlockPhysicsEvent,
     * dann aber über eine FallingBlock-Entität umgesetzt – dieser Handler
     * schließt die zweite Lücke.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (FALLING_ENTITIES.contains(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // 3) Feuer-Ausbreitung
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // 4) Eis schmelzen / Schnee schmelzen (BlockFadeEvent)
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // 5) Gras- / Pflanzen-Wachstum
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // 6) Wasser / Lava fließen
    // -------------------------------------------------------------------------

    /**
     * Verhindert das Fließen von Wasser und Lava in benachbarte Felder.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Material mat = event.getBlock().getType();
        if (mat == Material.WATER || mat == Material.STATIONARY_WATER
                || mat == Material.LAVA || mat == Material.STATIONARY_LAVA) {
            // Nur horizontales/vertikales Ausbreiten blockieren;
            // DOWN erlauben wir für bereits fließendes Wasser (optional, hier alles canceln)
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // 7) Feuer-Entzündung durch Block-Updates
    // -------------------------------------------------------------------------

    /**
     * Verhindert, dass Feuer neue Blöcke entzündet (Ergänzung zu BlockSpreadEvent).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        BlockIgniteEvent.IgniteCause cause = event.getCause();
        // Natürliche Ausbreitung und Blitze blockieren; manuelles Entzünden erlauben
        if (cause == BlockIgniteEvent.IgniteCause.SPREAD
                || cause == BlockIgniteEvent.IgniteCause.LAVA) {
            event.setCancelled(true);
        }
    }
}