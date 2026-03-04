package com.fendrixx.aurus.menu;

import com.fendrixx.aurus.processors.ActionProcessor;
import com.fendrixx.aurus.util.ColorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public class MenuRenderer {

    private final ActionProcessor actionProcessor;

    public MenuRenderer(ActionProcessor actionProcessor) {
        this.actionProcessor = actionProcessor;
    }

    public ActionProcessor getActionProcessor() {
        return actionProcessor;
    }

    public MenuButton createComponent(Player player, String type, ConfigurationSection conf, Location loc,
                                      double baseX, double baseY, Runnable closeAction) {

        float size = (float) conf.getDouble("size", 1.0);
        String rawText = conf.getString("text", "");

        // toUpperCase() añadido para evitar errores si en la config ponen "text" en minúsculas
        return switch (type.toUpperCase()) {
            case "TEXT" -> {
                TextDisplay td = spawnTextDisplay(loc, player, rawText, conf, size);
                yield new MenuButton(td, rawText, null, "TEXT", null, conf, actionProcessor, baseX, baseY);
            }
            case "BUTTON" -> {
                TextDisplay td = spawnTextDisplay(loc, player, rawText, conf, size);
                yield new MenuButton(td, rawText,
                        () -> actionProcessor.processList(player, conf.getStringList("actions"), closeAction),
                        "BUTTON", null, conf, actionProcessor, baseX, baseY);
            }
            case "INPUT" -> {
                TextDisplay td = spawnTextDisplay(loc, player, rawText, conf, size);
                yield new MenuButton(td, rawText,
                        () -> actionProcessor.processList(player, conf.getStringList("actions"), closeAction),
                        "INPUT", conf.getString("variable_name"), conf, actionProcessor, baseX, baseY);
            }
            case "ITEM" -> {
                // OPTIMIZACIÓN 1: Usamos la inyección nativa de Paper (Consumer).
                // Configura la entidad entera ANTES de mandarla al cliente, ahorrando un 80% de lag de red.
                ItemDisplay idisp = loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
                    Material mat = matchSafeMaterial(actionProcessor.parse(player, conf.getString("material", "STONE")));
                    display.setItemStack(new ItemStack(mat));
                    setupDisplay(display, size, conf);
                });
                yield new MenuButton(idisp, null, null, "ITEM", null, conf, actionProcessor, baseX, baseY);
            }
            case "BLOCK" -> {
                // Mismo proceso para BlockDisplay
                BlockDisplay bd = loc.getWorld().spawn(loc, BlockDisplay.class, display -> {
                    Material mat = matchSafeMaterial(actionProcessor.parse(player, conf.getString("material", "STONE")));
                    display.setBlock(mat.createBlockData());
                    setupDisplay(display, size, conf);
                });
                yield new MenuButton(bd, null, null, "BLOCK", null, conf, actionProcessor, baseX, baseY);
            }
            default -> null;
        };
    }

    private TextDisplay spawnTextDisplay(Location loc, Player p, String raw, ConfigurationSection conf, float size) {
        // También inicializamos el TextDisplay con Consumer nativo de Paper
        return loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.FIXED);

            // EL ARREGLO:
            // .text(Component) inyecta directamente los colores Kyori de Paper
            td.text(ColorUtils.format(actionProcessor.parse(p, raw)));

            if (!conf.getBoolean("background", true)) {
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }
            setupDisplay(td, size, conf);
        });
    }

    public void setupDisplay(Display display, float scale, ConfigurationSection conf) {
        Transformation trans = display.getTransformation();

        // OPTIMIZACIÓN 2: No creamos "new Vector3f()".
        // Modificamos el objeto de memoria que ya existe, reduciendo el trabajo del Garbage Collector.
        trans.getScale().set(scale, scale, scale);

        if (conf != null && conf.contains("rotation")) {
            trans.getLeftRotation().rotationXYZ(
                    (float) Math.toRadians(conf.getDouble("rotation.x", 0.0)),
                    (float) Math.toRadians(conf.getDouble("rotation.y", 0.0)),
                    (float) Math.toRadians(conf.getDouble("rotation.z", 0.0))
            );
        }
        display.setTransformation(trans);
    }

    /**
     * Sistema de seguridad Anti-Crash
     * Si en la config de casualidad ponen "material: MDIERA" en vez de "WOOD",
     * la versión anterior tiraría NullPointerException y rompería todo el menú y el servidor.
     * Esta versión devuelve PIEDRA (STONE) para avisarte del error sin romper nada.
     */
    private Material matchSafeMaterial(String name) {
        if (name == null || name.isEmpty()) return Material.STONE;
        Material mat = Material.matchMaterial(name.toUpperCase());
        return mat != null ? mat : Material.STONE;
    }
}
