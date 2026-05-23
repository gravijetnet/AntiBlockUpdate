package net.gravijet.lobby.portal;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortalManager {

    private final Plugin plugin;
    // BUG-26: store portals keyed by lower-case name for case-insensitive lookup
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    // BUG-25: initialise to non-null so savePortals() is safe even before loadPortals()
    private File portalsFile;
    private YamlConfiguration portalsConfig = new YamlConfiguration();

    // BUG-27/28: per-world portal index to skip irrelevant portals in O(1)
    private final Map<String, List<Portal>> portalsByWorld = new ConcurrentHashMap<>();

    private PortalListener portalListener;

    private static class Selection {
        private final String world1;
        private final String world2;
        private final Vector point1;
        private final Vector point2;

        private Selection(String world1, String world2, Vector point1, Vector point2) {
            this.world1 = world1;
            this.world2 = world2;
            this.point1 = point1;
            this.point2 = point2;
        }

        public String getWorldName() { return world1; }
        public Vector getPoint1() { return point1; }
        public Vector getPoint2() { return point2; }

        public boolean isComplete() {
            return point1 != null && point2 != null;
        }

        public boolean isSameWorld() {
            return world1 != null && world1.equals(world2);
        }

        public static Selection withPoint(Selection existing, String worldName, Vector point, int index) {
            String w1 = existing != null ? existing.world1 : null;
            String w2 = existing != null ? existing.world2 : null;
            Vector p1 = existing != null ? existing.point1 : null;
            Vector p2 = existing != null ? existing.point2 : null;
            if (index == 1) { p1 = point; w1 = worldName; }
            else { p2 = point; w2 = worldName; }
            return new Selection(w1, w2, p1, p2);
        }
    }

    public PortalManager(Plugin plugin) {
        this.plugin = plugin;
        portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        loadPortals();
    }

    /** Register the listener so reloadPortals() can clear stale player-portal state. */
    public void setPortalListener(PortalListener listener) {
        this.portalListener = listener;
    }

    public void loadPortals() {
        portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            try {
                plugin.saveResource("portals.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().info("portals.yml not found in JAR, creating empty file.");
                portalsConfig = new YamlConfiguration();
                try {
                    portalsConfig.save(portalsFile);
                } catch (IOException ex) {
                    // BUG-23: log the failure; portalsFile still doesn't exist but
                    // loadConfiguration below will handle a missing file gracefully
                    plugin.getLogger().severe("Could not create portals.yml: " + ex.getMessage());
                }
            }
        }
        portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
        portals.clear();
        portalsByWorld.clear();

        List<Map<?, ?>> portalsList = portalsConfig.getMapList("portals");
        for (Map<?, ?> map : portalsList) {
            try {
                Map<String, Object> data = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                Portal portal = Portal.deserialize(data);
                putPortal(portal);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load portal: " + map + " — " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + portals.size() + " portal(s).");
    }

    public void reloadPortals() {
        loadPortals();
        // BUG-17: clear stale player→portal mappings so re-entry triggers execute() again
        if (portalListener != null) {
            portalListener.clearAllPortalState();
        }
    }

    public void savePortals() {
        // BUG-25: portalsFile is always non-null (initialised in constructor + loadPortals)
        List<Map<String, Object>> list = new ArrayList<>();
        for (Portal portal : portals.values()) {
            list.add(portal.serialize());
        }
        portalsConfig.set("portals", list);
        try {
            portalsConfig.save(portalsFile);
        } catch (IOException e) {
            // BUG-21: surface the full exception so the admin can diagnose disk/permission issues
            plugin.getLogger().severe("Could not save portals.yml: " + e.getMessage());
        }
    }

    public boolean createPortal(String name, PortalType type, String value, String worldName, Vector min, Vector max) {
        String key = name.toLowerCase();
        if (portals.containsKey(key)) return false;
        Portal portal = new Portal(name, type, value, worldName, min, max);
        if (portals.putIfAbsent(key, portal) != null) return false;
        addToWorldIndex(portal);
        savePortals();
        return true;
    }

    public boolean deletePortal(String name) {
        Portal removed = portals.remove(name.toLowerCase());
        if (removed != null) {
            removeFromWorldIndex(removed);
            savePortals();
            return true;
        }
        return false;
    }

    public Portal getPortal(String name) {
        return portals.get(name.toLowerCase());
    }

    public Collection<Portal> getAllPortals() {
        return portals.values();
    }

    // BUG-27/28: only scan portals in the relevant world
    public Portal getPortalAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        List<Portal> worldPortals = portalsByWorld.get(location.getWorld().getName());
        if (worldPortals == null) return null;
        for (Portal portal : worldPortals) {
            if (portal.contains(location)) return portal;
        }
        return null;
    }

    public void setSelection(Player player, Location location, int point) {
        if (location.getWorld() == null) return;
        UUID uuid = player.getUniqueId();
        Selection updated = Selection.withPoint(selections.get(uuid), location.getWorld().getName(), location.toVector(), point);
        selections.put(uuid, updated);
    }

    // BUG-24: Javadoc notes that elements may be null; use hasCompleteSelection before calling
    public Vector[] getSelection(Player player) {
        Selection sel = selections.get(player.getUniqueId());
        if (sel == null) return new Vector[2];
        return new Vector[]{sel.getPoint1(), sel.getPoint2()};
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    public boolean hasCompleteSelection(Player player) {
        Selection sel = selections.get(player.getUniqueId());
        return sel != null && sel.isComplete();
    }

    public String getSelectionWorld(Player player) {
        Selection sel = selections.get(player.getUniqueId());
        return sel != null ? sel.getWorldName() : null;
    }

    public boolean createPortalFromSelection(Player player, String name, PortalType type, String value) {
        Selection sel = selections.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) return false;
        if (!sel.isSameWorld()) return false;
        String key = name.toLowerCase();
        Portal portal = new Portal(name, type, value, sel.getWorldName(), sel.getPoint1(), sel.getPoint2());
        // BUG-16/22: use putIfAbsent so this is atomic; reject if another thread already created it
        if (portals.putIfAbsent(key, portal) != null) return false;
        addToWorldIndex(portal);
        savePortals();
        clearSelection(player);
        return true;
    }

    // ── World index helpers ────────────────────────────────────────────────────

    private void putPortal(Portal portal) {
        portals.put(portal.getName().toLowerCase(), portal);
        addToWorldIndex(portal);
    }

    private void addToWorldIndex(Portal portal) {
        portalsByWorld.computeIfAbsent(portal.getWorldName(), k -> Collections.synchronizedList(new ArrayList<>()))
                      .add(portal);
    }

    private void removeFromWorldIndex(Portal portal) {
        List<Portal> worldPortals = portalsByWorld.get(portal.getWorldName());
        if (worldPortals != null) {
            worldPortals.remove(portal);
        }
    }
}
