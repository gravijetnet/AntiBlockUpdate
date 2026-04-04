package net.gravijet.lobby.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
            Bukkit.getLogger().warning("[Portal] Location has no world: " + location);
            return false;
        }
        if (!location.getWorld().getName().equals(worldName)) {
            Bukkit.getLogger().warning("[Portal] World mismatch. Portal world: " + worldName + ", Location world: " + location.getWorld().getName());
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        boolean inPortal = x >= min.getX() && x <= max.getX()
                && y >= min.getY() && y <= max.getY()
                && z >= min.getZ() && z <= max.getZ();

        if (inPortal) {
            Bukkit.getLogger().info("[Portal] Location " + x + "," + y + "," + z + " is INSIDE portal '" + name + "'");
            Bukkit.getLogger().info("[Portal] Bounds: min=" + min + " max=" + max);
        }

        return inPortal;
    }

    /**
     * Execute the portal action for the given player name.
     * @param playerName the player to affect
     */
    public void execute(String playerName) {
        Bukkit.getLogger().info("[Portal] Executing portal '" + name + "' for player " + playerName);
        Bukkit.getLogger().info("[Portal] Type: " + type + ", Value: " + value);

        switch (type) {
            case SERVER:
                sendToServer(playerName, value);
                break;
            case COMMAND:
                // Run as console, replace {player} placeholder
                String command = value.replace("{player}", playerName);
                Bukkit.getLogger().info("[Portal] Executing command: " + command);
                try {
                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    Bukkit.getLogger().info("[Portal] Command dispatch result: " + success);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[Portal] Failed to execute command: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            default:
                Bukkit.getLogger().warning("[Portal] Unknown portal type: " + type);
                break;
        }
    }

    /**
     * Send player to another server using BungeeCord/Velocity plugin messaging.
     */
    private void sendToServer(String playerName, String serverName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            Bukkit.getLogger().warning("[Portal] Player " + playerName + " not found online");
            return;
        }

        Bukkit.getLogger().info("[Portal] Attempting to send " + playerName + " to server " + serverName);

        // Try BungeeCord plugin messaging first
        if (tryBungeeCordConnect(player, serverName)) {
            return;
        }

        // Try Velocity plugin messaging
        if (tryVelocityConnect(player, serverName)) {
            return;
        }

        // Fallback to console command
        Bukkit.getLogger().warning("[Portal] Plugin messaging not available, falling back to console command");
        String serverCommand = "server " + playerName + " " + serverName;
        try {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), serverCommand);
            Bukkit.getLogger().info("[Portal] Console command result: " + success);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Portal] All connection methods failed: " + e.getMessage());
        }
    }

    /**
     * Try connecting via BungeeCord plugin messaging.
     */
    private boolean tryBungeeCordConnect(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(Bukkit.getPluginManager().getPlugin("AntiBlockUpdate"),
                    "BungeeCord", b.toByteArray());

            Bukkit.getLogger().info("[Portal] Sent BungeeCord connect request for " + serverName);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Portal] BungeeCord messaging failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Try connecting via Velocity plugin messaging.
     */
    private boolean tryVelocityConnect(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF(serverName);

            player.sendPluginMessage(Bukkit.getPluginManager().getPlugin("AntiBlockUpdate"),
                    "velocity:main", b.toByteArray());

            Bukkit.getLogger().info("[Portal] Sent Velocity connect request for " + serverName);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Portal] Velocity messaging failed: " + e.getMessage());
            return false;
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