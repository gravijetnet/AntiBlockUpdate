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
    private final Map<String, Portal> portals = new ConcurrentHashMap<>();
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    private File portalsFile;
    private YamlConfiguration portalsConfig;

    private static class Selection {
        private final String worldName;
        private final Vector point1;
        private final Vector point2;

        private Selection(String worldName, Vector point1, Vector point2) {
            this.worldName = worldName;
            this.point1 = point1;
            this.point2 = point2;
        }

        public String getWorldName() { return worldName; }
        public Vector getPoint1() { return point1; }
        public Vector getPoint2() { return point2; }

        public boolean isComplete() {
            return point1 != null && point2 != null;
        }

        public static Selection withPoint(Selection existing, String worldName, Vector point, int index) {
            Vector p1 = existing != null ? existing.point1 : null;
            Vector p2 = existing != null ? existing.point2 : null;
            if (index == 1) p1 = point;
            else p2 = point;
            return new Selection(worldName, p1, p2);
        }
    }

    public PortalManager(Plugin plugin) {
        this.plugin = plugin;
        loadPortals();
    }

    public void loadPortals() {
        portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            plugin.saveResource("portals.yml", false);
        }
        portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
        portals.clear();

        List<Map<?, ?>> portalsList = portalsConfig.getMapList("portals");
        for (Map<?, ?> map : portalsList) {
            try {
                Map<String, Object> data = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                Portal portal = Portal.deserialize(data);
                portals.put(portal.getName(), portal);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load portal: " + map);
            }
        }
        plugin.getLogger().info("Loaded " + portals.size() + " portal(s).");
    }

    public void reloadPortals() {
        loadPortals();
    }

    public void savePortals() {
        List<Map<String, Object>> portalsList = new ArrayList<>();
        for (Portal portal : portals.values()) {
            portalsList.add(portal.serialize());
        }
        portalsConfig.set("portals", portalsList);
        try {
            portalsConfig.save(portalsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save portals.yml!");
        }
    }

    public boolean createPortal(String name, PortalType type, String value, String worldName, Vector min, Vector max) {
        if (portals.containsKey(name)) return false;
        portals.put(name, new Portal(name, type, value, worldName, min, max));
        savePortals();
        return true;
    }

    public boolean deletePortal(String name) {
        if (portals.remove(name) != null) {
            savePortals();
            return true;
        }
        return false;
    }

    public Portal getPortal(String name) {
        return portals.get(name);
    }

    public Collection<Portal> getAllPortals() {
        return portals.values();
    }

    public Portal getPortalAt(Location location) {
        for (Portal portal : portals.values()) {
            if (portal.contains(location)) return portal;
        }
        return null;
    }

    public void setSelection(Player player, Location location, int point) {
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
        if (!hasCompleteSelection(player)) return false;
        Selection sel = selections.get(player.getUniqueId());
        portals.put(name, new Portal(name, type, value, sel.getWorldName(), sel.getPoint1(), sel.getPoint2()));
        savePortals();
        clearSelection(player);
        return true;
    }
}
