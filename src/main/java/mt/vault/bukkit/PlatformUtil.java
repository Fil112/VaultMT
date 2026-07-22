package mt.vault.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlatformUtil {

    public static void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runSync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Проверяет, является ли текущий сервер старой версией (1.0 - 1.12)
     */
    public static boolean isLegacy() {
        try {
            // Bukkit.getBukkitVersion() обычно возвращает строку вида "1.8.8-R0.1-SNAPSHOT"
            String version = Bukkit.getBukkitVersion().split("-")[0];
            String[] split = version.split("\\.");
            int minor = Integer.parseInt(split[1]); // Получаем второе число (например, 8 из 1.8.8)

            return minor < 13;
        } catch (Exception e) {
            // Если на совсем древнем ядре парсинг сломается,
            // для безопасности считаем сервер старым
            return true;
        }
    }
}