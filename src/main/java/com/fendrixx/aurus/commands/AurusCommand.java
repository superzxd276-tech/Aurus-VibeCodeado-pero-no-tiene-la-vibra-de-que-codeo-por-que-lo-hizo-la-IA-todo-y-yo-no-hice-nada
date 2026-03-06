package com.fendrixx.aurus.commands;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AurusCommand implements CommandExecutor, TabCompleter {

    private final Aurus plugin;

    // OPTIMIZACIÓN EXTREMA: Pre-compilamos los mensajes estáticos para que enviarlos cueste 0 de CPU
    private static final String PREFIX = "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>] ";

    private static final Component MSG_RELOAD = ColorUtils.format(PREFIX + "<dark_gray>[<green>✔<dark_gray>] <green>Configuration and menus reloaded.");
    private static final Component MSG_ONLY_PLAYERS = ColorUtils.format(PREFIX + "<dark_gray>[<red>✘<dark_gray>] <red>Only players can use this subcommand.");
    private static final Component MSG_USAGE_OPEN = ColorUtils.format(PREFIX + "<dark_gray>[<yellow>?<dark_gray>] <gray>Usage: /aurus open <menu_id>");
    private static final Component MSG_USAGE_DELETE = ColorUtils.format(PREFIX + "<dark_gray>[<yellow>?<dark_gray>] <gray>Usage: /aurus delete <menu_id>");
    private static final Component MSG_MENU_CLOSED = ColorUtils.format(PREFIX + "<dark_gray>[<green>✔<dark_gray>] <green>Menu closed.");
    private static final Component MSG_NO_MENU = ColorUtils.format(PREFIX + "<dark_gray>[<yellow>!<dark_gray>] <red>You don't have an active menu.");
    private static final Component MSG_HELP = ColorUtils.format(PREFIX + "<dark_gray>[<yellow>?<dark_gray>] <gray>Available commands: <white>open, close, delete, reload");
    private static final Component MSG_NO_PERMS = ColorUtils.format(PREFIX + "<dark_gray>[<red>✘<dark_gray>] <red>You do not have permission.");

    // Pre-lista de argumentos raíz para el TabCompleter para evitar crear Listas innecesarias
    private static final List<String> ROOT_ARGS = List.of("open", "reload", "close", "delete");

    public AurusCommand(Aurus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // En 1.20.1 comprobamos el permiso al inicio del todo
        if (!sender.hasPermission("aurus.admin")) {
            sender.sendMessage(MSG_NO_PERMS);
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            // Reload (Permitimos que la consola lo ejecute también)
            if (sub.equals("reload")) {
                plugin.getMenuManager().closeAll();
                plugin.getConfigHandler().reload();
                sender.sendMessage(MSG_RELOAD);
                return true;
            }

            // Delete (Permitimos que la consola lo ejecute también)
            if (sub.equals("delete")) {
                if (args.length < 2) {
                    sender.sendMessage(MSG_USAGE_DELETE);
                    return true;
                }

                String menuId = args[1];
                if (plugin.getConfigHandler().getMenuSection(menuId) == null) {
                    sender.sendMessage(ColorUtils.format(PREFIX + "<dark_gray>[<red>✘<dark_gray>] <red>Menu <yellow>" + menuId + "</yellow> not found."));
                    return true;
                }

                // Llamamos al método (que agregaremos a continuación en tu ConfigHandler)
                plugin.getConfigHandler().deleteMenu(menuId);
                sender.sendMessage(ColorUtils.format(PREFIX + "<dark_gray>[<green>✔<dark_gray>] <green>Menu <yellow>" + menuId + "</yellow> deleted successfully."));
                return true;
            }

            // Los demás comandos (open/close) obligan a que sea un Jugador
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MSG_ONLY_PLAYERS);
                return true;
            }

            if (sub.equals("open")) {
                if (args.length < 2) {
                    player.sendMessage(MSG_USAGE_OPEN);
                    return true;
                }

                String menuId = args[1];
                if (plugin.getConfigHandler().getMenuSection(menuId) == null) {
                    // Este es el único mensaje que procesamos en el momento por tener una variable
                    player.sendMessage(ColorUtils.format(PREFIX + "<dark_gray>[<red>✘<dark_gray>] <red>Menu <yellow>" + menuId + "</yellow> not found."));
                    return true;
                }

                plugin.getMenuManager().openMenu(player, menuId);
                return true;
            }

            if (sub.equals("close")) {
                if (plugin.getMenuManager().getActiveMenu(player.getUniqueId()) != null) {
                    plugin.getMenuManager().closeMenu(player);
                    player.sendMessage(MSG_MENU_CLOSED);
                } else {
                    player.sendMessage(MSG_NO_MENU);
                }
                return true;
            }
        }

        // Si no puso argumentos
        sender.sendMessage(MSG_HELP);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        // Si no tiene permiso, no le sugerimos nada (Seguridad)
        if (!sender.hasPermission("aurus.admin")) return List.of();

        // Autocompletado ultra-rápido usando Streams nativos de Java
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return ROOT_ARGS.stream().filter(s -> s.startsWith(input)).toList();
        }
        // Si el comando es "open" o "delete", le sugerimos los nombres de los menús
        else if (args.length == 2 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("delete"))) {
            String input = args[1].toLowerCase();
            return plugin.getConfigHandler().getMenuKeys().stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .toList();
        }

        return List.of();
    }
}
