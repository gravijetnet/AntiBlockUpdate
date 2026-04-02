package net.gravijet.lobby.portal;

import org.bukkit.ChatColor;
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

/**
 * Handles /portal command.
 */
public class PortalCommand implements CommandExecutor, TabCompleter {

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

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand":
                cmdWand(sender);
                break;
            case "create":
                cmdCreate(sender, args);
                break;
            case "delete":
                cmdDelete(sender, args);
                break;
            case "list":
                cmdList(sender);
                break;
            case "info":
                cmdInfo(sender, args);
                break;
            case "reload":
                cmdReload(sender);
                break;
            default:
                sendHelp(sender);
                break;
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
    }

    private void cmdWand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        Player player = (Player) sender;
        player.getInventory().addItem(new ItemStack(Material.BLAZE_ROD, 1));
        player.sendMessage("§c§lGraviJet §7» §fYou have received the selection wand.");
        player.sendMessage("§7Right-click a block to set first point, sneak + right-click for second point.");
    }

    private void cmdCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can create portals.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /portal create <name> <type> <value>");
            sender.sendMessage("§7Types: SERVER, COMMAND");
            return;
        }
        Player player = (Player) sender;
        String name = args[1];
        String typeStr = args[2].toUpperCase();
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        PortalType type;
        try {
            type = PortalType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid portal type. Use SERVER or COMMAND.");
            return;
        }

        if (!portalManager.hasCompleteSelection(player)) {
            sender.sendMessage("§cYou need to select two points with the wand first.");
            return;
        }

        if (portalManager.getPortal(name) != null) {
            sender.sendMessage("§cA portal with that name already exists.");
            return;
        }

        boolean success = portalManager.createPortalFromSelection(player, name, type, value);
        if (success) {
            sender.sendMessage("§c§lGraviJet §7» §fPortal '" + name + "' created successfully.");
        } else {
            sender.sendMessage("§cFailed to create portal.");
        }
    }

    private void cmdDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /portal delete <name>");
            return;
        }
        String name = args[1];
        boolean deleted = portalManager.deletePortal(name);
        if (deleted) {
            sender.sendMessage("§c§lGraviJet §7» §fPortal '" + name + "' deleted.");
        } else {
            sender.sendMessage("§cPortal not found.");
        }
    }

    private void cmdList(CommandSender sender) {
        List<Portal> portals = new ArrayList<>(portalManager.getAllPortals());
        if (portals.isEmpty()) {
            sender.sendMessage("§c§lGraviJet §7» §fNo portals defined.");
            return;
        }
        sender.sendMessage("§c§lGraviJet §7» §fPortals (" + portals.size() + "):");
        for (Portal portal : portals) {
            sender.sendMessage("§7- §f" + portal.getName() + " §8(" + portal.getType() + "§8)");
        }
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /portal info <name>");
            return;
        }
        String name = args[1];
        Portal portal = portalManager.getPortal(name);
        if (portal == null) {
            sender.sendMessage("§cPortal not found.");
            return;
        }
        sender.sendMessage("§c§lGraviJet §7» §fPortal Info:");
        sender.sendMessage("§7Name: §f" + portal.getName());
        sender.sendMessage("§7Type: §f" + portal.getType());
        sender.sendMessage("§7Value: §f" + portal.getValue());
        sender.sendMessage("§7World: §f" + portal.getWorldName());
        Vector min = portal.getMin();
        Vector max = portal.getMax();
        sender.sendMessage("§7Bounds: §f" + formatVector(min) + " → " + formatVector(max));
    }

    private String formatVector(Vector v) {
        return String.format("(%d, %d, %d)", (int) v.getX(), (int) v.getY(), (int) v.getZ());
    }

    private void cmdReload(CommandSender sender) {
        portalManager.reloadPortals();
        sender.sendMessage("§c§lGraviJet §7» §fPortals reloaded.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("wand", "create", "delete", "list", "info", "reload");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("delete") || sub.equals("info")) {
                // Tab complete portal names
                return portalManager.getAllPortals().stream()
                        .map(Portal::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("create")) {
                // No completion for name
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            // Tab complete portal types
            return Arrays.stream(PortalType.values())
                    .map(Enum::name)
                    .filter(type -> type.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}