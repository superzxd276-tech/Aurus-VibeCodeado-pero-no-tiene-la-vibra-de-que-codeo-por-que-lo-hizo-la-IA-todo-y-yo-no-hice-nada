package com.fendrixx.aurus.listeners;

import com.fendrixx.aurus.Aurus;
import com.fendrixx.aurus.menu.Menu;
import com.fendrixx.aurus.menu.MenuButton;
import com.fendrixx.aurus.util.MathUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionListener implements Listener {

    private final Aurus plugin;
    // Usamos ConcurrentHashMap porque PacketEvents podría procesar paquetes de forma asíncrona
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    private static final double DEG_TO_RAD = Math.PI / 180.0;

    public InteractionListener(Aurus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());

        if (menu == null) return;

        event.setCancelled(true);
        if (event.getAction() == Action.PHYSICAL) return;

        handleMenuInteraction(player, menu);
    }

    public void handle3DClick(Player player) {
        Menu menu = plugin.getMenuManager().getActiveMenu(player.getUniqueId());
        if (menu == null) return;

        handleMenuInteraction(player, menu);
    }

    private void handleMenuInteraction(Player player, Menu menu) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // OPTIMIZACIÓN: getOrDefault evita hacer containsKey() y get() (ahorra una búsqueda)
        if (now - lastClick.getOrDefault(uuid, 0L) < 250) return;
        lastClick.put(uuid, now);

        float pYaw = player.getLocation().getYaw();
        float pPitch = player.getLocation().getPitch();
        float cYaw = menu.getSpawnYaw();
        float cPitch = menu.getSpawnPitch();
        double dist = menu.getMenuDistance();

        float dYaw = MathUtil.normalizeAngle(pYaw - cYaw);
        float dPitch = MathUtil.normalizeAngle(pPitch - cPitch);

        double cursorX = Math.tan(dYaw * DEG_TO_RAD) * dist;
        double cursorY = -Math.tan(dPitch * DEG_TO_RAD) * dist;

        for (MenuButton btn : menu.getButtons()) {
            double size = btn.getConfig().getDouble("size", 1.0);
            double hitRadius = 0.5 * size;

            double dx = cursorX - btn.getBaseX();
            double dy = cursorY - btn.getBaseY();

            // OPTIMIZACIÓN EXTREMA: Jamás uses Math.sqrt() en bucles grandes, es lentísimo.
            // En vez de eso, calculamos la distancia al cuadrado y la comparamos con el radio al cuadrado.
            double distanceSquared = (dx * dx) + (dy * dy);
            double hitRadiusSquared = hitRadius * hitRadius;

            if (distanceSquared < hitRadiusSquared) {
                if ("INPUT".equalsIgnoreCase(btn.getType())) {
                    plugin.getInputProcessor().startInput(player, btn.getVariableName());
                }
                btn.onClick();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                break;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // PREVENCIÓN DE MEMORY LEAKS: Si se va, lo borramos de la memoria
        lastClick.remove(player.getUniqueId());
        plugin.getMenuManager().closeMenu(player);
    }
}
