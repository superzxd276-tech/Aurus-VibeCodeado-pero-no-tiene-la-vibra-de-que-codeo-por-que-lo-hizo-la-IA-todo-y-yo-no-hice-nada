package com.fendrixx.aurus;

import com.fendrixx.aurus.commands.AurusCommand;
import com.fendrixx.aurus.config.ConfigHandler;
import com.fendrixx.aurus.expansion.PAPIExpansion;
import com.fendrixx.aurus.listeners.InteractionListener;
import com.fendrixx.aurus.listeners.MoveListener;
import com.fendrixx.aurus.menu.MenuManager;
import com.fendrixx.aurus.processors.ActionProcessor;
import com.fendrixx.aurus.processors.InputProcessor;
import com.fendrixx.aurus.util.ColorUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Aurus extends JavaPlugin {

    private MenuManager menuManager;
    private ConfigHandler configHandler;
    private ActionProcessor actionProcessor;
    private InputProcessor inputProcessor;

    @Override
    public void onLoad() {
        // Inicialización optimizada de PacketEvents
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        // Desactivamos bStats y updates en PacketEvents para ahorrar milisegundos y red
        PacketEvents.getAPI().getSettings().bStats(false).checkForUpdates(false);
        PacketEvents.getAPI().load();

        // Paper nativo 1.19+: Usamos el ComponentLogger, mucho más rápido que Bukkit.getConsoleSender()
        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>][<yellow>↺<dark_gray>] <yellow>Plugin loading..."));
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();

        this.configHandler = new ConfigHandler(this);
        this.menuManager = new MenuManager(this);
        this.actionProcessor = new ActionProcessor(this);
        this.inputProcessor = new InputProcessor(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this.inputProcessor, this).register();
        }

        registerCommands();

        InteractionListener interactionListener = new InteractionListener(this);
        getServer().getPluginManager().registerEvents(interactionListener, this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(this.inputProcessor, this);

        registerPacketListener(interactionListener);

        sendStartupMessage();
    }

    @Override
    public void onDisable() {
        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>][<yellow>↺<dark_gray>] <yellow>Disabling menus..."));
        if (this.menuManager != null) {
            this.menuManager.closeAll();
        }

        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>] [<yellow>↺<dark_gray>] <yellow>Disabling packetevents..."));
        PacketEvents.getAPI().terminate();

        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>][<green>✔<dark_gray>] <green>Plugin disabled!"));
    }

    private void registerCommands() {
        // Método clásico para 1.20.1
        if (getCommand("aurus") != null) {
            AurusCommand cmd = new AurusCommand(this);
            getCommand("aurus").setExecutor(cmd);
            getCommand("aurus").setTabCompleter(cmd);
        }
    }

    private void registerPacketListener(InteractionListener listener) {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

                    // Verificación moderna de Pattern Matching de Java 16+
                    if (event.getPlayer() instanceof Player player) {
                        if (menuManager.getActiveMenu(player.getUniqueId()) != null) {
                            event.setCancelled(true);

                            // OPTIMIZACIÓN EXTREMA 1.19+ (Soporta Folia y Multi-hilos):
                            // Usamos el Entity Scheduler de Paper en vez del viejo Bukkit.getScheduler()
                            player.getScheduler().run(Aurus.this, task -> listener.handle3DClick(player), null);
                        }
                    }
                }
            }
        });
    }

    private void sendStartupMessage() {
        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>] [<green>✔<dark_gray>] <green>Plugin enabled!"));
        this.getComponentLogger().info(ColorUtils.format(
                "<dark_gray>[<gradient:dark_purple:yellow> Aurus </gradient><dark_gray>][<yellow>!<dark_gray>] <gold>ty for using my plugin! <dark_gray>~ Fendrixx"));

        // Usamos Bukkit Console Sender para este bloque multilinea porque los Loggers de Paper
        // le ponen prefijos "[INFO]" a cada salto de línea y arruina el arte ASCII.
        Bukkit.getConsoleSender().sendMessage(ColorUtils.format("""
                \s
                <gradient:dark_purple:yellow>| ░█▀▀█⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀</gradient>
                <gradient:dark_purple:yellow>| ░█▄▄█░█ ░█ █▀▀█░█ ░█░██▀▀</gradient>
                <gradient:dark_purple:yellow>| ░█ ░█░█▄▄█ █▀█▄░█▄▄█ ▄▄█▀</gradient>
                <dark_purple>|<gradient:dark_gray:white> - Aurus 1.0.0-BETA by Fendrixx
                \s
                """));
    }

    public MenuManager getMenuManager() {
        return this.menuManager;
    }

    public ConfigHandler getConfigHandler() {
        return this.configHandler;
    }

    public ActionProcessor getActionProcessor() {
        return this.actionProcessor;
    }

    public InputProcessor getInputProcessor() {
        return this.inputProcessor;
    }
}
