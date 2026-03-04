package com.fendrixx.aurus.menu;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.util.ColorUtils;
import com.fendrixx.aurus.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Menu {

    private final Aurus plugin;
    private final Player player;
    private final MenuCamera camera;
    private final MenuRenderer renderer;
    private final List<MenuButton> buttons = new ArrayList<>();

    private Display cursorEntity;
    private MenuAnimator animator;
    private Location oldLocation;
    private double menuDistance;
    private Location menuOrigin;
    private float spawnYaw;
    private float spawnPitch;
    private boolean closed = false;

    // OPTIMIZACIÓN: Creamos el efecto de poción una sola vez (es una constante)
    // Evita instanciar new PotionEffect(...) cada vez que alguien abre un menú
    private static final PotionEffect INVISIBILITY_EFFECT = new PotionEffect(
            PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false
    );

    public Menu(Aurus plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.camera = new MenuCamera(player);
        // OPTIMIZACIÓN: Reutilizamos el ActionProcessor cacheado de la clase principal
        // ¡Ya no creamos una nueva instancia inútil por cada menú abierto!
        this.renderer = new MenuRenderer(plugin.getActionProcessor());
    }

    public void open(String menuId) {
        ConfigurationSection section = plugin.getConfigHandler().getMenuSection(menuId);
        if (section == null) return;

        this.oldLocation = player.getLocation().clone();
        this.menuDistance = section.getDouble("distance", 2.5);

        Location savedLocation = player.getLocation().clone();
        camera.spawn();

        // PAPER API: teleportAsync() evita que el servidor dé un lagazo si el chunk está descargado
        player.teleportAsync(savedLocation);

        this.spawnYaw = player.getLocation().getYaw();
        this.spawnPitch = player.getLocation().getPitch();
        this.menuOrigin = MathUtil.getMenuOrigin(camera.getEyeLocation(), spawnYaw, spawnPitch, menuDistance);

        spawnCursor();

        ConfigurationSection comps = section.getConfigurationSection("components");
        if (comps != null) {
            for (String key : comps.getKeys(false)) {
                ConfigurationSection c = comps.getConfigurationSection(key);
                double bx = c.getDouble("x");
                double by = c.getDouble("y");
                Location loc = calculateComponentLocation(bx, by);

                MenuButton btn = renderer.createComponent(player, c.getString("type", "BUTTON").toUpperCase(),
                        c, loc, bx, by, this::close);

                if (btn != null) {
                    buttons.add(btn);
                }
            }
        }

        // OPTIMIZACIÓN DE BUCLES (O(N) en lugar de O(N*M)):
        // Ocultamos todas las entidades en UN SOLO BUCLE por jugador, en lugar de hacerlo por cada botón.
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hideEntity(plugin, player); // Esconde al dueño del menú
                if (cursorEntity != null) {
                    otherPlayer.hideEntity(plugin, cursorEntity); // Esconde el cursor
                }
                for (MenuButton btn : buttons) {
                    otherPlayer.hideEntity(plugin, btn.getDisplay()); // Esconde los botones
                }
            }
        }

        player.hideEntity(plugin, player);
        player.addPotionEffect(INVISIBILITY_EFFECT);

        // NOTA SOBRE FOLIA:
        // Si tu clase MenuAnimator extiende BukkitRunnable, recuerda actualizarla luego para que use
        // el EntityScheduler de Paper en vez del Scheduler global.
        int delay = section.getInt("update-in-ticks", 20);
        this.animator = new MenuAnimator(this, player, buttons, menuDistance, delay);
        this.animator.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnCursor() {
        ConfigurationSection c = plugin.getConfigHandler().getCursorSection();
        Location loc = menuOrigin.clone();
        loc.setYaw(spawnYaw + 180f);
        loc.setPitch(-spawnPitch);

        String valRaw = c != null ? c.getString("value", "●") : "●";
        String parsedVal = renderer.getActionProcessor().parse(player, valRaw);

        // OPTIMIZACIÓN (Consumer) y ARREGLO (.text(Component)) combinados:
        // Spawneamos la entidad de una sola vez con los colores y config ya aplicados.
        this.cursorEntity = player.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.FIXED);

            // EL ARREGLO MÁGICO DE PAPER:
            td.text(ColorUtils.format(parsedVal));

            if (c == null || !c.getBoolean("background", true)) {
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }
            renderer.setupDisplay(td, (float) (c != null ? c.getDouble("size", 1.0) : 1.0), c);
        });
    }

    public Location calculateComponentLocation(double x, double y) {
        return MathUtil.calculateComponentLocation(menuOrigin, spawnYaw, spawnPitch, x, y);
    }

    public void close() {
        if (closed) return;
        closed = true;

        if (animator != null) animator.cancel();

        // Despawn rápido de entidades
        camera.remove();
        if (cursorEntity != null) cursorEntity.remove();
        buttons.forEach(b -> b.getDisplay().remove());
        buttons.clear();

        plugin.getMenuManager().removeMenu(player.getUniqueId());

        if (player.isOnline()) {
            player.showEntity(plugin, player);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            if (oldLocation != null) {
                player.teleportAsync(oldLocation); // Uso seguro para Folia/Paper
            }

            for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                if (!otherPlayer.equals(player)) {
                    otherPlayer.showEntity(plugin, player);
                }
            }
        }
    }

    public void updateVisuals() {
        for (MenuButton btn : buttons) {
            btn.updateText(player);
        }
    }

    public MenuCamera getCamera() {
        return camera;
    }

    public Display getCursor() {
        return cursorEntity;
    }

    public List<MenuButton> getButtons() {
        return buttons;
    }

    public Location getMenuOrigin() {
        return menuOrigin;
    }

    public float getSpawnYaw() {
        return spawnYaw;
    }

    public float getSpawnPitch() {
        return spawnPitch;
    }

    public double getMenuDistance() {
        return menuDistance;
    }
}
