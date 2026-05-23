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
    // Keyed by lower-case portal name for case-insensitive lookup.
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    private final File portalsFile;
    private YamlConfiguration portalsConfig = new YamlConfiguration();

    // Per-world portal index to avoid scanning every portal on every move event.
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
            else            { p2 = point; w2 = worldName; }
            return new Selection(w1, w2, p1, p2);
        }
    }

    public PortalManager(Plugin plugin) {
        this.plugin = plugin;
        // Fix #34: assign portalsFile once in the constructor; loadPortals() no longer reassigns it.
        portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        loadPortals();
    }

    public void setPortalListener(PortalListener listener) {
        this.portalListener = listener;
    }

    public void loadPortals() {
        // Fix #32/#33: build the new portal set into a temporary map first, then swap
        // atomically so a mid-parse failure never leaves the live map half-populated.
        if (!portalsFile.exists()) {
            try {
                plugin.saveResource("portals.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().info("portals.yml not found in JAR, creating empty file.");
                portalsConfig = new YamlConfiguration();
                try {
                    portalsConfig.save(portalsFile);
                } catch (IOException ex) {
                    plugin.getLogger().severe("Could not create portals.yml: " + ex.getMessage());
                }
            }
        }

        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(portalsFile);
        if (newConfig.getKeys(false).isEmpty() && portalsFile.exists() && portalsFile.length() > 0) {
            // loadConfiguration returned an empty config despite the file having content —
            // this indicates a read error; bail out rather than wiping live portal data.
            plugin.getLogger().severe("Could not read portals.yml — keeping current portal data.");
            return;
        }

        Map<String, Portal> newPortals = new HashMap<>();
        Map<String, List<Portal>> newByWorld = new HashMap<>();

        List<Map<?, ?>> portalsList = newConfig.getMapList("portals");
        for (Map<?, ?> map : portalsList) {
            try {
                Map<String, Object> data = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                Portal portal = Portal.deserialize(data);
                newPortals.put(portal.getName().toLowerCase(), portal);
                newByWorld.computeIfAbsent(portal.getWorldName(), k -> new ArrayList<>()).add(portal);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load portal: " + map + " — " + e.getMessage());
            }
        }

        // Swap in the fully-parsed data atomically.
        portals.clear();
        portals.putAll(newPortals);
        portalsByWorld.clear();
        // Wrap each list in a synchronizedList to match the concurrent contract in addToWorldIndex.
        for (Map.Entry<String, List<Portal>> entry : newByWorld.entrySet()) {
            portalsByWorld.put(entry.getKey(), Collections.synchronizedList(entry.getValue()));
        }

        portalsConfig = newConfig;
        plugin.getLogger().info("Loaded " + portals.size() + " portal(s).");
    }

    public void reloadPortals() {
        loadPortals();
        if (portalListener != null) {
            portalListener.clearAllPortalState();
        }
    }

    public void savePortals() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Portal portal : portals.values()) {
            list.add(portal.serialize());
        }
        portalsConfig.set("portals", list);
        try {
            portalsConfig.save(portalsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save portals.yml: " + e.getMessage());
        }
    }

    public boolean createPortal(String name, PortalType type, String value, String worldName, Vector min, Vector max) {
        String key = name.toLowerCase();
        Portal portal = new Portal(name, type, value, worldName, min, max);
        // Fix #36: use only putIfAbsent — remove the redundant containsKey pre-check.
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

    // Fix #27: only scan portals in the relevant world.
    public Portal getPortalAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        List<Portal> worldPortals = portalsByWorld.get(location.getWorld().getName());
        if (worldPortals == null) return null;
        // Fix #37: synchronize iteration over the synchronizedList to prevent
        // ConcurrentModificationException when addToWorldIndex/removeFromWorldIndex run concurrently.
        synchronized (worldPortals) {
            for (Portal portal : worldPortals) {
                if (portal.contains(location)) return portal;
            }
        }
        return null;
    }

    public void setSelection(Player player, Location location, int point) {
        if (location.getWorld() == null) return;
        UUID uuid = player.getUniqueId();
        Selection updated = Selection.withPoint(selections.get(uuid), location.getWorld().getName(), location.toVector(), point);
        selections.put(uuid, updated);
    }

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
        if (portals.putIfAbsent(key, portal) != null) return false;
        addToWorldIndex(portal);
        savePortals();
        clearSelection(player);
        return true;
    }

    // ── World index helpers ────────────────────────────────────────────────────

    private void addToWorldIndex(Portal portal) {
        portalsByWorld.computeIfAbsent(portal.getWorldName(), k -> Collections.synchronizedList(new ArrayList<>()))
                      .add(portal);
    }

    private void removeFromWorldIndex(Portal portal) {
        List<Portal> worldPortals = portalsByWorld.get(portal.getWorldName());
        if (worldPortals != null) {
            // Fix #37: synchronize removal to match the synchronizedList contract.
            synchronized (worldPortals) {
                worldPortals.remove(portal);
            }
        }
    }
}
