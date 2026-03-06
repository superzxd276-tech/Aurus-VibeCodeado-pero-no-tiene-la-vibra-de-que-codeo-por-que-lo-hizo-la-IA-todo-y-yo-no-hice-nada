package com.fendrixx.aurus.menu;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.util.MathUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuAnimator {

    private final Aurus plugin;
    private final Menu menu;
    private final Player player;
    private final List<MenuButton> buttons;
    private final double distance;
    private final int updateDelay;
    private double ticks = 0;
    private int updateCounter = 0;

    private ScheduledTask task;
    private final Map<MenuButton, ButtonAnimData> cache = new HashMap<>();

    public MenuAnimator(Aurus plugin, Menu menu, Player player, List<MenuButton> buttons, double distance, int updateDelay) {
        this.plugin = plugin;
        this.menu = menu;
        this.player = player;
        this.buttons = buttons;
        this.distance = distance;
        this.updateDelay = updateDelay;

        for (MenuButton btn : buttons) {
            ConfigurationSection conf = btn.getConfig();
            ConfigurationSection anim = conf.getConfigurationSection("animations");

            double baseX = conf.getDouble("x", 0.0);
            double baseY = conf.getDouble("y", 0.0);

            String scaleF = anim != null ? anim.getString("scale-formula") : null;
            String rotF = anim != null ? anim.getString("rotation-formula") : null;
            String xF = anim != null ? anim.getString("x-formula") : null;
            String yF = anim != null ? anim.getString("y-formula") : null;

            Display display = btn.getDisplay();
            // OPTIMIZACIÓN GC: Guardamos la transformación base al inicio.
            Transformation baseTransform = display != null ? display.getTransformation() : null;

            cache.put(btn, new ButtonAnimData(anim != null, baseX, baseY, scaleF, rotF, xF, yF, baseTransform));
        }
    }

    public void start() {
        this.task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> tick(), null, 1L, 1L);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
        }
    }

    private void tick() {
        if (menu.getCamera().getTripod() == null || !player.isOnline() || player.isDead()) {
            menu.close();
            stop();
            return;
        }

        ticks += 0.05;

        for (MenuButton btn : buttons) {
            ButtonAnimData data = cache.get(btn);
            if (!data.hasAnimations) continue;

            Display display = btn.getDisplay();
            if (display == null || display.isDead()) continue;

            Transformation trans = data.cachedTransform;
            if (trans == null) {
                trans = display.getTransformation();
                data.cachedTransform = trans;
            }

            boolean changed = false;

            if (data.scaleFormula != null) {
                float s = (float) MathUtil.evaluate(data.scaleFormula, ticks);
                trans.getScale().set(s, s, s);
                changed = true;
            }

            if (data.rotationFormula != null) {
                float r = (float) Math.toRadians(MathUtil.evaluate(data.rotationFormula, ticks));
                trans.getLeftRotation().identity().rotateZ(r);
                changed = true;
            }

            if (data.xFormula != null || data.yFormula != null) {
                double rx = data.xFormula != null ? MathUtil.evaluate(data.xFormula, ticks) : 0;
                double ry = data.yFormula != null ? MathUtil.evaluate(data.yFormula, ticks) : 0;

                Location offsetLoc = menu.calculateComponentLocation(data.baseX + rx, data.baseY + ry);

                // COMPATIBILIDAD 1.20.1: Quitamos setTeleportDuration
                // Si actualizas tu API a 1.20.2+, puedes poner display.setTeleportDuration(1); aquí.
                display.teleport(offsetLoc);
            }

            if (changed) {
                // El tamaño y la rotación SÍ son fluidos en 1.20.1
                display.setInterpolationDuration(1);
                display.setInterpolationDelay(0);
                display.setTransformation(trans);
            }
        }

        Display cursor = menu.getCursor();
        if (cursor != null && !cursor.isDead()) {
            Location newCursorPos = MathUtil.getCursorLocation(
                    menu.getMenuOrigin(),
                    menu.getSpawnYaw(),
                    menu.getSpawnPitch(),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch(),
                    distance);

            // COMPATIBILIDAD 1.20.1: Quitamos setTeleportDuration
            cursor.teleport(newCursorPos);
        }

        updateCounter++;
        if (updateCounter >= updateDelay) {
            buttons.forEach(b -> b.updateText(player));
            updateCounter = 0;
        }
    }

    private static class ButtonAnimData {
        final boolean hasAnimations;
        final double baseX;
        final double baseY;
        final String scaleFormula;
        final String rotationFormula;
        final String xFormula;
        final String yFormula;
        Transformation cachedTransform;

        ButtonAnimData(boolean hasAnimations, double baseX, double baseY, String scaleFormula, String rotationFormula, String xFormula, String yFormula, Transformation cachedTransform) {
            this.hasAnimations = hasAnimations;
            this.baseX = baseX;
            this.baseY = baseY;
            this.scaleFormula = scaleFormula;
            this.rotationFormula = rotationFormula;
            this.xFormula = xFormula;
            this.yFormula = yFormula;
            this.cachedTransform = cachedTransform;
        }
    }
}
