package net.gravijet.lobby.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Portal {

    private final String name;
    private final PortalType type;
    private final String value;
    private final String worldName;
    private final Vector min;
    private final Vector max;

    public Portal(String name, PortalType type, String value, String worldName, Vector min, Vector max) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.worldName = worldName;
        // Store bounds as block-integer coordinates to avoid sub-block precision ambiguity.
        this.min = new Vector(
                (int) Math.floor(Math.min(min.getX(), max.getX())),
                (int) Math.floor(Math.min(min.getY(), max.getY())),
                (int) Math.floor(Math.min(min.getZ(), max.getZ())));
        this.max = new Vector(
                (int) Math.floor(Math.max(min.getX(), max.getX())),
                (int) Math.floor(Math.max(min.getY(), max.getY())),
                (int) Math.floor(Math.max(min.getZ(), max.getZ())));
    }

    public String getName() { return name; }
    public PortalType getType() { return type; }
    public String getValue() { return value; }
    public String getWorldName() { return worldName; }
    public Vector getMin() { return min.clone(); }
    public Vector getMax() { return max.clone(); }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equals(worldName)) return false;
        int bx = (int) Math.floor(location.getX());
        int by = (int) Math.floor(location.getY());
        int bz = (int) Math.floor(location.getZ());
        return bx >= (int) min.getX() && bx <= (int) max.getX()
            && by >= (int) min.getY() && by <= (int) max.getY()
            && bz >= (int) min.getZ() && bz <= (int) max.getZ();
    }

    public boolean execute(Player player, Plugin plugin) {
        switch (type) {
            case SERVER:
                if (!player.isOnline()) return false;
                return sendToServer(player, value, plugin);
            case COMMAND:
                String safeName = player.getName().replaceAll("[^A-Za-z0-9_]", "");
                if (safeName.isEmpty()) {
                    plugin.getLogger().warning("Portal '" + name + "': player name became empty after sanitisation, skipping command.");
                    return false;
                }
                if (value.isEmpty()) {
                    plugin.getLogger().warning("Portal '" + name + "': command value is empty, skipping.");
                    return false;
                }
                String cmd = value.replace("{player}", player.getUniqueId().toString())
                                  .replace("{player_name}", safeName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                return true;
            default:
                plugin.getLogger().warning("Portal '" + name + "': unhandled portal type '" + type + "', cannot execute.");
                return false;
        }
    }

    private boolean sendToServer(Player player, String serverName, Plugin plugin) {
        if (serverName.isEmpty()) {
            plugin.getLogger().warning("Portal '" + name + "': server value is empty, skipping.");
            return false;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF("Connect");
            out.writeUTF(serverName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to build BungeeCord message for player "
                    + player.getName() + ": " + e.getMessage());
            if (player.isOnline()) player.sendMessage("§cPortal error: could not connect to server. Please try again.");
            return false;
        }
        if (!player.isOnline()) return false;
        player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        return true;
    }

    // Fix #20: use LinkedHashMap for stable field order in the serialized YAML output.
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("type", type.name());
        data.put("value", value);
        data.put("world", worldName);
        data.put("min_x", min.getX());
        data.put("min_y", min.getY());
        data.put("min_z", min.getZ());
        data.put("max_x", max.getX());
        data.put("max_y", max.getY());
        data.put("max_z", max.getZ());
        return data;
    }

    public static Portal deserialize(Map<String, Object> data) {
        String name    = requireString(data, "name");
        String world   = requireString(data, "world");
        String value   = requireString(data, "value");
        String typeStr = requireString(data, "type");
        PortalType type;
        try {
            type = PortalType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown portal type '" + typeStr + "' for portal '" + name + "'", e);
        }
        double minX = requireFiniteNumber(data, "min_x");
        double minY = requireFiniteNumber(data, "min_y");
        double minZ = requireFiniteNumber(data, "min_z");
        double maxX = requireFiniteNumber(data, "max_x");
        double maxY = requireFiniteNumber(data, "max_y");
        double maxZ = requireFiniteNumber(data, "max_z");
        return new Portal(name, type, value, world,
                new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ));
    }

    private static String requireString(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (!(val instanceof String))
            throw new IllegalArgumentException("Missing or invalid field '" + key + "'");
        String s = (String) val;
        if (s.isEmpty())
            throw new IllegalArgumentException("Field '" + key + "' must not be empty");
        return s;
    }

    private static double requireFiniteNumber(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (!(val instanceof Number))
            throw new IllegalArgumentException("Missing or invalid numeric field '" + key + "'");
        double d = ((Number) val).doubleValue();
        if (!Double.isFinite(d))
            throw new IllegalArgumentException("Non-finite value for field '" + key + "': " + d);
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portal other = (Portal) o;
        return name.equals(other.name)
                && type == other.type
                && value.equals(other.value)
                && worldName.equals(other.worldName)
                && min.equals(other.min)
                && max.equals(other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value, worldName, min, max);
    }

    @Override
    public String toString() {
        return "Portal{name='" + name + "', type=" + type + ", value='" + value + "', world='" + worldName + "', min=" + min + ", max=" + max + '}';
    }
}
