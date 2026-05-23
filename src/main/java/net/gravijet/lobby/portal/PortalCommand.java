package net.gravijet.lobby.portal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PortalCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE    = "antiblockupdate.portal.use";
    private static final String PERM_MANAGE = "antiblockupdate.portal.manage";

    // BUG-15: max length for a portal name
    private static final int MAX_NAME_LENGTH = 32;

    private final PortalManager portalManager;

    public PortalCommand(PortalManager portalManager) {
        this.portalManager = portalManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "wand":   cmdWand(sender); break;
            case "create": cmdCreate(sender, args); break;
            case "delete": cmdDelete(sender, args); break;
            case "list":   cmdList(sender); break;
            case "info":   cmdInfo(sender, args); break;
            case "reload": cmdReload(sender); break;
            case "test":
            case "debug":  cmdTest(sender); break;
            default: sendHelp(sender); break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§c§lPortal Help §7- §fUseful commands.");
        sender.sendMessage(" §7● §c/portal wand §7» §fGet the selection wand (Blaze Rod).");
        sender.sendMessage(" §7● §c/portal create <name> <type> <value> §7» §fCreate a portal from your selection.");
        sender.sendMessage(" §7● §c/portal delete <name> §7» §fDelete a portal.");
        sender.sendMessage(" §7● §c/portal list §7» §fList all portals.");
        sender.sendMessage(" §7● §c/portal info <name> §7» §fShow portal details.");
        sender.sendMessage(" §7● §c/portal reload §7» §fReload portals from file.");
        sender.sendMessage(" §7● §c/portal test §7» §fCheck if you're inside a portal.");
    }

    private void cmdWand(CommandSender sender) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_USE)) { sender.sendMessage("§cYou do not have permission."); return; }
        if (!(sender instanceof Player)) { sender.sendMessage("§cOnly players can use this command."); return; }
        Player player = (Player) sender;
        player.getInventory().addItem(new ItemStack(Material.BLAZE_ROD, 1));
        player.sendMessage("§c§lGraviJet §7» §fYou received the selection wand.");
        player.sendMessage("§7Right-click to set point 1, sneak + right-click for point 2.");
    }

    private void cmdCreate(CommandSender sender, String[] args) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_MANAGE)) { sender.sendMessage("§cYou do not have permission."); return; }
        if (!(sender instanceof Player)) { sender.sendMessage("§cOnly players can create portals."); return; }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /portal create <name> <type> <value>");
            sender.sendMessage("§7Types: SERVER, COMMAND");
            return;
        }
        Player player = (Player) sender;
        String name = args[1];

        // BUG-15: validate portal name
        if (!isValidName(name)) {
            sender.sendMessage("§cInvalid portal name. Use only letters, digits, hyphens, underscores (max " + MAX_NAME_LENGTH + " chars).");
            return;
        }

        PortalType type;
        try {
            type = PortalType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid type. Use SERVER or COMMAND.");
            return;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        if (!portalManager.hasCompleteSelection(player)) {
            sender.sendMessage("§cSelect two points with the wand first.");
            return;
        }
        // BUG-16: createPortalFromSelection now uses putIfAbsent internally, making the
        // create atomic. The check here is a fast-path UX guard only.
        if (portalManager.getPortal(name) != null) {
            sender.sendMessage("§cA portal with that name already exists.");
            return;
        }
        if (portalManager.createPortalFromSelection(player, name, type, value)) {
            sender.sendMessage("§c§lGraviJet §7» §fPortal '" + name + "' created.");
        } else {
            sender.sendMessage("§cFailed to create portal. Both selection points must be in the same world, and the name must not already exist.");
        }
    }

    private void cmdDelete(CommandSender sender, String[] args) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_MANAGE)) { sender.sendMessage("§cYou do not have permission."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /portal delete <name>"); return; }
        if (portalManager.deletePortal(args[1])) {
            sender.sendMessage("§c§lGraviJet §7» §fPortal '" + args[1] + "' deleted.");
        } else {
            sender.sendMessage("§cPortal not found.");
        }
    }

    private void cmdList(CommandSender sender) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_MANAGE)) { sender.sendMessage("§cYou do not have permission."); return; }
        List<Portal> portals = new ArrayList<>(portalManager.getAllPortals());
        if (portals.isEmpty()) { sender.sendMessage("§c§lGraviJet §7» §fNo portals defined."); return; }
        sender.sendMessage("§c§lGraviJet §7» §fPortals (" + portals.size() + "):");
        for (Portal p : portals) {
            sender.sendMessage("§7- §f" + p.getName() + " §8(" + p.getType() + "§8)");
        }
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_MANAGE)) { sender.sendMessage("§cYou do not have permission."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /portal info <name>"); return; }
        Portal portal = portalManager.getPortal(args[1]);
        if (portal == null) { sender.sendMessage("§cPortal not found."); return; }
        sender.sendMessage("§c§lGraviJet §7» §fPortal Info:");
        sender.sendMessage("§7Name: §f" + portal.getName());
        sender.sendMessage("§7Type: §f" + portal.getType());
        sender.sendMessage("§7Value: §f" + portal.getValue());
        sender.sendMessage("§7World: §f" + portal.getWorldName());
        sender.sendMessage("§7Bounds: §f" + fv(portal.getMin()) + " → " + fv(portal.getMax()));
    }

    private void cmdReload(CommandSender sender) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_MANAGE)) { sender.sendMessage("§cYou do not have permission."); return; }
        portalManager.reloadPortals();
        sender.sendMessage("§c§lGraviJet §7» §fPortals reloaded.");
    }

    private void cmdTest(CommandSender sender) {
        // BUG-14: permission check
        if (!sender.hasPermission(PERM_USE)) { sender.sendMessage("§cYou do not have permission."); return; }
        if (!(sender instanceof Player)) { sender.sendMessage("§cOnly players can use this command."); return; }
        Player player = (Player) sender;
        Location location = player.getLocation();
        Portal portal = portalManager.getPortalAt(location);
        if (portal != null) {
            sender.sendMessage("§c§lGraviJet §7» §aInside portal: §e" + portal.getName());
            sender.sendMessage("§7Type: §f" + portal.getType() + " §7Value: §f" + portal.getValue());
            sender.sendMessage("§7Bounds: §f" + fv(portal.getMin()) + " → " + fv(portal.getMax()));
        } else {
            sender.sendMessage("§c§lGraviJet §7» §fNot inside any portal.");
            String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
            sender.sendMessage("§7Position: §f" + fv(location.toVector()) + " §7World: §f" + worldName);
            List<Portal> portals = new ArrayList<>(portalManager.getAllPortals());
            if (!portals.isEmpty()) {
                sender.sendMessage("§7Portals: §f" + portals.stream().map(Portal::getName).collect(Collectors.joining(", ")));
            }
        }
    }

    private String fv(Vector v) {
        return String.format("(%d, %d, %d)", (int) v.getX(), (int) v.getY(), (int) v.getZ());
    }

    // BUG-15: alphanumeric + hyphen + underscore, bounded length, no color codes
    private boolean isValidName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= MAX_NAME_LENGTH
                && name.matches("[A-Za-z0-9_\\-]+");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            // BUG-16 (tab): use a mutable list so removeIf works correctly
            List<String> subs = new ArrayList<>(Arrays.asList("wand", "create", "delete", "list", "info", "reload", "test"));
            subs.removeIf(s -> !s.startsWith(prefix));
            return subs;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("delete") || sub.equals("info")) {
                return portalManager.getAllPortals().stream()
                        .map(Portal::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(PortalType.values())
                    .map(Enum::name)
                    .filter(t -> t.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
