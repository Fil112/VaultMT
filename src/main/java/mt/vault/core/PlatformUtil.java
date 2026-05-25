package mt.vault.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlatformUtil {

    private static final boolean IS_FOLIA = checkFolia();

    // Проверяем, есть ли классы Folia в ядре сервера
    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Универсальный метод для запуска асинхронных задач (БД, подсчеты)
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            // Для Folia (1.20+)
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            // Для Spigot/Paper (1.16 - 26.1)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Универсальный метод для запуска синхронных задач (отправка сообщений)
     */
    public static void runSync(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            // В Folia глобального синхронного потока нет, используем GlobalRegion
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}