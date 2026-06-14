package mt.vault.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlatformUtil {
    public static void runAsync(Plugin plugin, Runnable task) {Bukkit.getScheduler().runTaskAsynchronously(plugin, task);}
    public static void runSync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}