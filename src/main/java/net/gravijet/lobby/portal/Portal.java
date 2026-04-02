package net.gravijet.lobby.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a portal defined by a cuboid region.
 */
@SerializableAs("Portal")
public class Portal implements ConfigurationSerializable {

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
        this.min = new Vector(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        this.max = new Vector(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
    }

    public String getName() {
        return name;
    }

    public PortalType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getWorldName() {
        return worldName;
    }

    public Vector getMin() {
        return min.clone();
    }

    public Vector getMax() {
        return max.clone();
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= min.getX() && x <= max.getX()
                && y >= min.getY() && y <= max.getY()
                && z >= min.getZ() && z <= max.getZ();
    }

    /**
     * Execute the portal action for the given player name.
     * @param playerName the player to affect
     */
    public void execute(String playerName) {
        switch (type) {
            case SERVER:
                // BungeeCord / Velocity server send
                String serverCommand = "server " + playerName + " " + value;
                Bukkit.getLogger().info("[Portal] Sending " + playerName + " to server " + value + " via command: " + serverCommand);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), serverCommand);
                break;
            case COMMAND:
                // Run as console, replace {player} placeholder
                String command = value.replace("{player}", playerName);
                Bukkit.getLogger().info("[Portal] Executing command: " + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            default:
                break;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("type", type.name());
        data.put("value", value);
        data.put("world", worldName);
        data.put("min", min);
        data.put("max", max);
        return data;
    }

    public static Portal deserialize(Map<String, Object> data) {
        String name = (String) data.get("name");
        PortalType type = PortalType.valueOf((String) data.get("type"));
        String value = (String) data.get("value");
        String world = (String) data.get("world");
        Vector min = (Vector) data.get("min");
        Vector max = (Vector) data.get("max");
        return new Portal(name, type, value, world, min, max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portal portal = (Portal) o;
        return name.equals(portal.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Portal{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", value='" + value + '\'' +
                ", worldName='" + worldName + '\'' +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}