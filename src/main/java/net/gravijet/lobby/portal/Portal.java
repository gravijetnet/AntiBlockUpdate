package net.gravijet.lobby.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
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
        this.min = new Vector(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        this.max = new Vector(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
    }

    public String getName() { return name; }
    public PortalType getType() { return type; }
    public String getValue() { return value; }
    public String getWorldName() { return worldName; }
    public Vector getMin() { return min.clone(); }
    public Vector getMax() { return max.clone(); }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) return false;
        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();
        return bx >= (int) min.getX() && bx <= (int) max.getX()
            && by >= (int) min.getY() && by <= (int) max.getY()
            && bz >= (int) min.getZ() && bz <= (int) max.getZ();
    }

    public void execute(Player player, Plugin plugin) {
        switch (type) {
            case SERVER:
                sendToServer(player, value, plugin);
                break;
            case COMMAND:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.replace("{player}", player.getName()));
                break;
        }
    }

    private void sendToServer(Player player, String serverName, Plugin plugin) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            player.kickPlayer("Connecting to " + serverName + "...");
        }
    }

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
        return name.equals(((Portal) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Portal{name='" + name + "', type=" + type + ", value='" + value + "', world='" + worldName + "', min=" + min + ", max=" + max + '}';
    }
}
