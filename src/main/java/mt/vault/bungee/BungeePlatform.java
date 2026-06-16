package mt.vault.bungee;

import mt.vault.core.VaultPlatform;
import net.md_5.bungee.api.ProxyServer;

import java.io.File;
import java.util.logging.Logger;

public class BungeePlatform implements VaultPlatform {
    private final VaultMTBungee plugin;
    private final Logger logger;

    public BungeePlatform(VaultMTBungee plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public File getPluginFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public void info(String message) { logger.info(message); }

    @Override
    public void warning(String message) { logger.warning(message); }

    @Override
    public void severe(String message) { logger.severe(message); }

    // TODO: Здесь позже нужно будет прикрутить чтение YAML-конфига BungeeCord.
    // Пока возвращаем дефолтные значения, чтобы ядро не сломалось.
    @Override
    public String getConfigString(String path, String def) { return def; }

    @Override
    public double getConfigDouble(String path, double def) { return def; }

    @Override
    public boolean getConfigBoolean(String path, boolean def) { return def; }

    @Override
    public void runAsync(Runnable task) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        // В BungeeCord нет главного потока, всё работает асинхронно
        runAsync(task);
    }
}