package mt.vault.sponge;

import mt.vault.core.VaultPlatform;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SpongePlatform implements VaultPlatform {

    private final Logger logger;
    private final Path configDir;
    private final PluginContainer plugin;
    private ConfigurationNode configNode;

    public SpongePlatform(Object pluginInstance, Logger logger, Path configDir) {
        this.plugin = Sponge.pluginManager().fromInstance(pluginInstance)
                .orElseThrow(() -> new RuntimeException("Не удалось найти PluginContainer!"));
        this.logger = logger;
        this.configDir = configDir;
        loadConfig();
    }

    private void loadConfig() {
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File configFile = new File(dir, "config.yml");

        // Копируем дефолтный конфиг
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    info("Стандартный config.yml успешно сгенерирован.");
                }
            } catch (Exception e) {
                severe("Ошибка создания config.yml: " + e.getMessage());
            }
        }

        // Загружаем конфиг через YAML Loader
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .build();

        try {
            configNode = loader.load();
        } catch (Exception e) {
            severe("Критическая ошибка при загрузке конфигурации Sponge: " + e.getMessage());
        }
    }

    @Override
    public File getPluginFolder() { return configDir.toFile(); }

    @Override
    public void info(String message) { logger.info(message); }

    @Override
    public void warning(String message) { logger.warn(message); }

    @Override
    public void severe(String message) { logger.error(message); }

    @Override
    public String getConfigString(String path, String def) {
        if (configNode == null) return def;
        return configNode.node((Object[]) path.split("\\.")).getString(def);
    }

    @Override
    public double getConfigDouble(String path, double def) {
        if (configNode == null) return def;
        return configNode.node((Object[]) path.split("\\.")).getDouble(def);
    }

    @Override
    public boolean getConfigBoolean(String path, boolean def) {
        if (configNode == null) return def;
        return configNode.node((Object[]) path.split("\\.")).getBoolean(def);
    }

    @Override
    public void runAsync(Runnable runnable) {
        Task task = Task.builder().plugin(plugin).execute(runnable).build();
        Sponge.asyncScheduler().submit(task);
    }

    @Override
    public void runSync(Runnable runnable) {
        Task task = Task.builder().plugin(plugin).execute(runnable).build();
        Sponge.server().scheduler().submit(task);
    }

    @Override
    public java.util.List<String> getConfigStringList(String path) {
        if (configNode == null) return java.util.Collections.emptyList();
        try {
            return configNode.node((Object[]) path.split("\\.")).getList(String.class, java.util.Collections.emptyList());
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}