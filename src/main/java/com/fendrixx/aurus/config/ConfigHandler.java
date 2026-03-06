package com.fendrixx.aurus.config;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigHandler {
    private final Aurus plugin;
    private FileConfiguration config;
    private final Map<String, FileConfiguration> menus = new HashMap<>();
    private final String prefix = "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>] ";

    public ConfigHandler(Aurus plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMenus();
    }

    public void loadConfig() {
        try {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            Bukkit.getConsoleSender().sendMessage(ColorUtils
                    .format(prefix + "<dark_gray>[<green>✔<dark_gray>] <green>Main config loaded successfully!"));
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils
                    .format(prefix + "<dark_gray>[<red>✘<dark_gray>] <red>Failed to load config: " + e.getMessage()));
            this.config = new YamlConfiguration();
        }
    }

    public void loadMenus() {
        menus.clear();
        File folder = new File(plugin.getDataFolder(), "menus");

        if (!folder.exists()) {
            folder.mkdirs();
            try {
                plugin.saveResource("menus/welcome_server.yml", false);
                plugin.saveResource("menus/user_profile.yml", false);
                plugin.saveResource("menus/name_menu.yml", false);
                plugin.saveResource("menus/animated_menu.yml", false);
                plugin.saveResource("menus/pixelart.yml", false);
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(ColorUtils
                        .format(prefix + "<dark_gray>[<yellow>!<dark_gray>] <gray>Could not save default menus."));
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                String name = file.getName().replace(".yml", "");
                YamlConfiguration menuConfig = new YamlConfiguration();
                menuConfig.load(file);
                menus.put(name, menuConfig);
                Bukkit.getConsoleSender().sendMessage(ColorUtils
                        .format(prefix + "<dark_gray>[<green>✔<dark_gray>] <gray>Loaded menu: <white>" + name));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(
                        ColorUtils.format(prefix + "<dark_gray>[<red>✘<dark_gray>] <red>Error loading <yellow>"
                                + file.getName() + "<red>: " + e.getMessage()));
            }
        }
    }

    public ConfigurationSection getMenuSection(String menuId) {
        if (menuId == null || menuId.isEmpty())
            return null;

        for (FileConfiguration menuFile : menus.values()) {
            if (menuFile.isConfigurationSection(menuId)) {
                return menuFile.getConfigurationSection(menuId);
            }
        }

        if (config != null && config.isConfigurationSection("menus." + menuId)) {
            return config.getConfigurationSection("menus." + menuId);
        }

        return null;
    }

    public Set<String> getMenuKeys() {
        Set<String> keys = new HashSet<>();
        for (FileConfiguration menuFile : menus.values()) {
            keys.addAll(menuFile.getKeys(false));
        }
        if (config != null && config.isConfigurationSection("menus")) {
            keys.addAll(config.getConfigurationSection("menus").getKeys(false));
        }
        return keys;
    }

    public ConfigurationSection getCursorSection() {
        return config != null ? config.getConfigurationSection("cursor") : null;
    }

    // NUEVO MÉTODO: Elimina un menú tanto de la memoria como del archivo físico
    public boolean deleteMenu(String menuId) {
        if (menuId == null || menuId.isEmpty()) return false;

        // 1. Buscamos el menú en la carpeta "menus"
        for (Map.Entry<String, FileConfiguration> entry : menus.entrySet()) {
            String fileName = entry.getKey();
            FileConfiguration menuFile = entry.getValue();

            if (menuFile.isConfigurationSection(menuId)) {
                menuFile.set(menuId, null); // Lo borramos de la memoria

                File file = new File(plugin.getDataFolder(), "menus/" + fileName + ".yml");
                try {
                    // Si el archivo quedó vacío después de borrar el menú, borramos el archivo físico
                    if (menuFile.getKeys(false).isEmpty()) {
                        if (file.exists()) {
                            file.delete();
                        }
                        menus.remove(fileName); // Lo sacamos del HashMap de cachés
                    } else {
                        menuFile.save(file); // Guardamos los cambios en el archivo
                    }
                    return true; // Terminamos aquí porque ya lo borramos
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage(ColorUtils.format(prefix + "<dark_gray>[<red>✘<dark_gray>] <red>Failed to delete menu file: " + e.getMessage()));
                    return false;
                }
            }
        }

        // 2. Si no estaba en la carpeta "menus", lo buscamos en el main "config.yml"
        if (config != null && config.isConfigurationSection("menus." + menuId)) {
            config.set("menus." + menuId, null);
            plugin.saveConfig(); // Guarda el config.yml
            return true;
        }

        return false; // El menú no existía
    }

    public void reload() {
        loadConfig();
        loadMenus();
    }
}
