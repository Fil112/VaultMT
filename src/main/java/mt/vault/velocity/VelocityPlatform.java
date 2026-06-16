package mt.vault.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import mt.vault.core.VaultPlatform;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class VelocityPlatform implements VaultPlatform {
    private final VaultMTVelocity plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    public VelocityPlatform(VaultMTVelocity plugin, ProxyServer server, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
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

    // TODO: Здесь позже прикрутим чтение TOML/HOCON конфига Velocity.
    @Override
    public String getConfigString(String path, String def) { return def; }

    @Override
    public double getConfigDouble(String path, double def) { return def; }

    @Override
    public boolean getConfigBoolean(String path, boolean def) { return def; }

    @Override
    public void runAsync(Runnable task) {
        server.getScheduler().buildTask(plugin, task).schedule();
    }

    @Override
    public void runSync(Runnable task) {
        runAsync(task);
    }
}