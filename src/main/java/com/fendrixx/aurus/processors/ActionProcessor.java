package com.fendrixx.aurus.processors;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionProcessor {

    private final Aurus plugin;
    // OPTIMIZACIÓN 1: Cacheamos si PAPI está activado.
    // Antes, el plugin le preguntaba al servidor si PAPI existía en CADA línea de texto ejecutada.
    private final boolean hasPapi;

    public ActionProcessor(Aurus plugin) {
        this.plugin = plugin;
        this.hasPapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void processList(Player player, List<String> actions, Runnable closeCallback) {
        if (actions == null || actions.isEmpty()) return;

        for (String action : actions) {
            process(player, action, closeCallback);
        }
    }

    public void process(Player player, String action, Runnable closeCallback) {
        if (action == null || action.isEmpty()) return;

        // Pasamos el texto por PlaceholderAPI (si está activo)
        String cmd = parse(player, action);

        // OPTIMIZACIÓN 2: Evitamos el .replace() si el texto no contiene %player%.
        // Hacer un .replace() a ciegas genera copias en memoria innecesarias.
        if (cmd.contains("%player%")) {
            cmd = cmd.replace("%player%", player.getName());
        }

        if (cmd.equalsIgnoreCase("[close]")) {
            if (closeCallback != null) closeCallback.run();
            return;
        }

        if (cmd.startsWith("[console] ")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(10));

        } else if (cmd.startsWith("[player] ")) {
            player.performCommand(cmd.substring(9));

        } else if (cmd.startsWith("[broadcast] ")) {
            // OPTIMIZACIÓN 3: Paper Native Messaging + Nuestro ColorUtils optimizado
            Bukkit.getServer().sendMessage(ColorUtils.format(cmd.substring(12)));

        } else if (cmd.startsWith("[message] ")) {
            // Paper Native Messaging (Adiós BukkitAudiences)
            player.sendMessage(ColorUtils.format(cmd.substring(10)));

        } else if (cmd.startsWith("[openmenu] ")) {
            String menuId = cmd.substring(11).trim();
            if (closeCallback != null) closeCallback.run();

            // OPTIMIZACIÓN 4: Entity Scheduler en vez de Global Scheduler
            // Esto asegura soporte para Folia y evita crasheos si el jugador se desconecta en ese lapso de 2 ticks.
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    plugin.getMenuManager().openMenu(player, menuId);
                }
            }, null, 2L);

        } else if (cmd.startsWith("[sound] ")) {
            parseAndPlaySound(player, cmd.substring(8));
        }
    }

    /**
     * OPTIMIZACIÓN 5: Análisis de Sonido sin crear Arrays ni Regex
     * El anterior .split(", ") creaba un Array de Strings (basura para el Garbage Collector).
     * Ahora usamos punteros directos (.indexOf) para leer los datos.
     */
    private void parseAndPlaySound(Player player, String soundData) {
        try {
            int firstComma = soundData.indexOf(',');

            if (firstComma == -1) {
                // Solo puso el nombre del sonido (Ej:[sound] ENTITY_EXPERIENCE_ORB_PICKUP)
                Sound sound = Sound.valueOf(soundData.trim().toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                return;
            }

            // Sacamos el nombre del sonido
            String soundName = soundData.substring(0, firstComma).trim().toUpperCase();
            Sound sound = Sound.valueOf(soundName);

            int secondComma = soundData.indexOf(',', firstComma + 1);
            float vol;
            float pitch = 1.0f;

            if (secondComma == -1) {
                // Puso sonido y volumen (Ej: [sound] ENTITY_EXPERIENCE_ORB_PICKUP, 0.5)
                vol = Float.parseFloat(soundData.substring(firstComma + 1).trim());
            } else {
                // Puso sonido, volumen y pitch (Ej: [sound] ENTITY_EXPERIENCE_ORB_PICKUP, 0.5, 1.2)
                vol = Float.parseFloat(soundData.substring(firstComma + 1, secondComma).trim());
                pitch = Float.parseFloat(soundData.substring(secondComma + 1).trim());
            }

            player.playSound(player.getLocation(), sound, vol, pitch);

        } catch (IllegalArgumentException ignored) {
            // Capturar específicamente IllegalArgumentException es mucho más rápido
            // que capturar la clase genérica 'Exception'.
        }
    }

    public String parse(Player player, String text) {
        if (text == null || text.isEmpty()) return "";

        if (hasPapi) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
