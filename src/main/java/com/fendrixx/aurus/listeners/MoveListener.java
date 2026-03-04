package com.fendrixx.aurus.listeners;

import com.fendrixx.aurus.Aurus;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    private final Aurus plugin;

    public MoveListener(Aurus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // MAGIA DE PAPER: Si el jugador solo rotó la cámara con el ratón, ignoramos el evento.
        // Esto ahorra el 90% del procesamiento inútil en el servidor.
        if (!event.hasChangedPosition()) return;

        Player player = event.getPlayer();

        // Si el jugador intenta caminar (WASD o saltar) mientras tiene un menú abierto:
        if (plugin.getMenuManager().getActiveMenu(player.getUniqueId()) != null) {

            Location from = event.getFrom();
            Location to = event.getTo();

            // OPTIMIZACIÓN MÁXIMA: No creamos Vectores, ni Distancias, ni HashMaps.
            // Simplemente sobreescribimos su destino en X, Y, Z con el lugar del que partió.
            // Conservamos el Yaw y Pitch del 'to' para que su cámara gire fluido.
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());

            // Nota: Mutar el objeto 'to' del evento es la forma más limpia de congelar a un
            // jugador sin causar "tirones" visuales ni lag en el cliente.
        }
    }
}
