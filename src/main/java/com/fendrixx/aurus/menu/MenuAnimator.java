package com.fendrixx.aurus.menu;

import com.fendrixx.aurus.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.util.List;

public class MenuAnimator extends BukkitRunnable {
    private final Menu menu;
    private final Player player;
    private final List<MenuButton> buttons;
    private final double distance;
    private final int updateDelay;
    private double ticks = 0;
    private int updateCounter = 0;

    public MenuAnimator(Menu menu, Player player, List<MenuButton> buttons, double distance, int updateDelay) {
        this.menu = menu;
        this.player = player;
        this.buttons = buttons;
        this.distance = distance;
        this.updateDelay = updateDelay;
    }

    @Override
    public void run() {
        if (menu.getCamera().getTripod() == null || !player.isOnline()) {
            menu.close();
            return;
        }

        ticks += 0.05;

        for (MenuButton btn : buttons) {
            ConfigurationSection conf = btn.getConfig();
            Display display = btn.getDisplay();

            if (conf.contains("animations")) {
                ConfigurationSection anim = conf.getConfigurationSection("animations");
                double x = conf.getDouble("x") + (anim.contains("x-formula") ? MathUtil.evaluate(anim.getString("x-formula"), ticks) : 0);
                double y = conf.getDouble("y") + (anim.contains("y-formula") ? MathUtil.evaluate(anim.getString("y-formula"), ticks) : 0);
                display.teleport(menu.calculateComponentLocation(x, y));

                Transformation trans = display.getTransformation();
                if (anim.contains("scale-formula")) {
                    float s = (float) MathUtil.evaluate(anim.getString("scale-formula"), ticks);
                    trans.getScale().set(s, s, s);
                }
                if (anim.contains("rotation-formula")) {
                    float r = (float) MathUtil.evaluate(anim.getString("rotation-formula"), ticks);
                    trans.getLeftRotation().rotationXYZ(0, 0, (float) Math.toRadians(r));
                }
                display.setTransformation(trans);
            }
        }


        Location newPos = MathUtil.getCursorLocation(menu.getCamera().getEyeLocation(), menu.getCamera().getLocation().getYaw(),
                player.getLocation().getYaw(), player.getLocation().getPitch(), distance);
        menu.getCursor().teleport(newPos);

        updateCounter++;
        if (updateCounter >= updateDelay) {
            buttons.forEach(b -> b.updateText(player));
            updateCounter = 0;
        }
    }
}