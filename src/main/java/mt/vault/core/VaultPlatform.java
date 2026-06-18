package mt.vault.core;

import java.io.File;

public interface VaultPlatform {
    File getPluginFolder();
    void info(String message);
    void warning(String message);
    void severe(String message);
    String getConfigString(String path, String def);
    double getConfigDouble(String path, double def);
    boolean getConfigBoolean(String path, boolean def);
    void runAsync(Runnable task);
    void runSync(Runnable task);
    java.util.List<String> getConfigStringList(String path);
}