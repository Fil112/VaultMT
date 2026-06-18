package mt.vault.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import mt.vault.core.VaultPlatform;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class VelocityPlatform implements VaultPlatform {

    private final Object plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigurationNode configNode;

    // Обновленный конструктор: теперь принимает инстанс плагина и ProxyServer
    public VelocityPlatform(Object plugin, ProxyServer server, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        loadConfig();
    }

    private void loadConfig() {
        File folder = dataDirectory.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File configFile = new File(folder, "config.yml");

        // Если конфига нет — достаем дефолтный из ресурсов JAR-файла
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    info("Файл config.yml успешно создан!");
                } else {
                    warning("Встроенный config.yml не найден в ресурсах плагина!");
                }
            } catch (Exception e) {
                severe("Не удалось скопировать config.yml: " + e.getMessage());
            }
        }

        // Загружаем конфиг в память с помощью Configurate
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile.toPath())
                    .build();
            configNode = loader.load();
        } catch (Exception e) {
            severe("Ошибка при чтении config.yml: " + e.getMessage());
        }
    }

    @Override
    public File getPluginFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public void info(String message) { logger.info(message); }

    @Override
    public void warning(String message) { logger.warn(message); }

    @Override
    public void severe(String message) { logger.error(message); }

    // Configurate разделяет пути по точкам с помощью массивов
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
    public List<String> getConfigStringList(String path) {
        if (configNode == null) return Collections.emptyList();
        try {
            return configNode.node((Object[]) path.split("\\.")).getList(String.class, Collections.emptyList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void runAsync(Runnable task) {
        // Теперь мы используем встроенный планировщик задач Velocity
        server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public void runSync(Runnable task) {
        // В Velocity архитектура полностью асинхронна, направляем в runAsync
        runAsync(task);
    }
}