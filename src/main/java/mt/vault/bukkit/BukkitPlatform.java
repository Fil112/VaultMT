package mt.vault.bukkit;

import mt.vault.core.VaultPlatform;

import java.io.File;

public class BukkitPlatform implements VaultPlatform {
    private final VaultMTP plugin;

    public BukkitPlatform(VaultMTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public File getPluginFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public void info(String message) {
        plugin.logInfo(message);
    }

    @Override
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    @Override
    public String getConfigString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    @Override
    public double getConfigDouble(String path, double def) {
        return plugin.getConfig().getDouble(path, def);
    }

    @Override
    public boolean getConfigBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    @Override
    public void runAsync(Runnable task) {
        PlatformUtil.runAsync(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        PlatformUtil.runSync(plugin, task);
    }
}