package mt.vault.bungee;

import mt.vault.core.VaultPlatform;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BungeePlatform implements VaultPlatform {
    private final VaultMTBungee plugin;
    private final Logger logger;
    private Configuration config;

    public BungeePlatform(VaultMTBungee plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        loadConfig();
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // Если файла нет, копируем стандартный из ресурсов JAR
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    info("Стандартный config.yml успешно сгенерирован.");
                } else {
                    warning("Встроенный config.yml не найден в ресурсах плагина!");
                }
            } catch (IOException e) {
                severe("Ошибка при создании config.yml: " + e.getMessage());
            }
        }

        // Загружаем конфиг через нативный провайдер BungeeCord
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            severe("Ошибка при чтении config.yml: " + e.getMessage());
        }
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

    @Override
    public String getConfigString(String path, String def) {
        return config != null ? config.getString(path, def) : def;
    }

    @Override
    public double getConfigDouble(String path, double def) {
        return config != null ? config.getDouble(path, def) : def;
    }

    @Override
    public boolean getConfigBoolean(String path, boolean def) {
        return config != null ? config.getBoolean(path, def) : def;
    }

    @Override
    public List<String> getConfigStringList(String path) {
        return config != null ? config.getStringList(path) : Collections.emptyList();
    }

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