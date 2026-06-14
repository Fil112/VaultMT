package mt.vault.sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

public class PlatformUtil {

    public static void runAsync(Runnable task) {
        // Получаем контейнер плагина через главный класс Sponge
        PluginContainer plugin = Sponge.pluginManager().fromInstance(VaultMTSponge.getInstance()).get();
        Task t = Task.builder().plugin(plugin).execute(task).build();
        Sponge.asyncScheduler().submit(t);
    }

    public static void runSync(Runnable task) {
        PluginContainer plugin = Sponge.pluginManager().fromInstance(VaultMTSponge.getInstance()).get();
        Task t = Task.builder().plugin(plugin).execute(task).build();
        Sponge.server().scheduler().submit(t);
    }
}