package com.fendrixx.aurus.listeners;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.menu.Menu;
import com.fendrixx.aurus.menu.MenuButton;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class InteractionListener implements Listener {
    private final Aurus plugin;

    public InteractionListener(Aurus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());

        if (menu == null) return;

        event.setCancelled(true);

        for (MenuButton btn : menu.getButtons()) {
            double dist = menu.getCursor().getLocation().distance(btn.getDisplay().getLocation());

            if (dist < 0.9) {
                if (btn.getType().equals("INPUT")) {
                    String variable = btn.getVariableName();

                    plugin.getInputProcessor().startInput(player, variable);
                }

                btn.onClick();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                break;
            }
        }
    }

    public void handle3DClick(Player player) {
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());
        if (menu == null) return;

        Location eye = player.getEyeLocation();
        Vector lookDir = eye.getDirection().normalize();

        MenuButton bestButton = null;
        double bestDot = -1.0;

        for (MenuButton btn : menu.getButtons()) {
            Location btnLoc = btn.getDisplay().getLocation();

            Vector toBtn = btnLoc.toVector().subtract(eye.toVector());
            double dist = toBtn.length();

            if (dist > 6.0) continue;

            double dot = lookDir.dot(toBtn.normalize());

            if (dot > 0.985 && dot > bestDot) {
                bestDot = dot;
                bestButton = btn;
            }
        }

        if (bestButton != null) {
            if ("INPUT".equalsIgnoreCase(bestButton.getType())) {
                plugin.getInputProcessor().startInput(player, bestButton.getVariableName());
            }

            bestButton.onClick();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Menu menu = plugin.getMenuManager().getActiveMenu(event.getPlayer().getUniqueId());
        if (menu != null) {
            menu.close();
        }
    }
}
