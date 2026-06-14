package mt.vault.sponge;

import mt.vault.core.VaultPlatform;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

public class SpongePlatform implements VaultPlatform {
    private final Logger logger;
    private final Path configDir;
    private final PluginContainer plugin;
    private CommentedConfigurationNode configNode;

    public SpongePlatform(Object pluginInstance, Logger logger, Path configDir) {
        this.plugin = Sponge.pluginManager().fromInstance(pluginInstance)
                .orElseThrow(() -> new RuntimeException("Не удалось найти PluginContainer!"));
        this.logger = logger;
        this.configDir = configDir;

        // Загружаем конфиг при старте платформы
        loadConfig();
    }

    private void loadConfig() {
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Sponge обычно использует формат HOCON (.conf)
        File configFile = new File(dir, "config.conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .path(configFile.toPath())
                .build();

        try {
            if (!configFile.exists()) {
                // Если файла нет, создаем его в памяти и сохраняем на диск
                configNode = loader.createNode();
                loader.save(configNode);
            } else {
                // Читаем существующий файл
                configNode = loader.load();
            }
        } catch (Exception e) {
            logger.error("Критическая ошибка при загрузке конфигурации Sponge", e);
        }
    }

    @Override
    public File getPluginFolder() {
        return configDir.toFile();
    }

    @Override
    public void info(String message) { logger.info(message); }

    @Override
    public void warning(String message) { logger.warn(message); }

    @Override
    public void severe(String message) { logger.error(message); }

    @Override
    public String getConfigString(String path, String def) {
        if (configNode == null) return def;
        // В Configurate пути разделяются массивом, поэтому сплитим по точке
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
}