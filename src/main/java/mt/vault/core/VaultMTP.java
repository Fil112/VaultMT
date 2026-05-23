package mt.vault.core;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class VaultMTP extends JavaPlugin {

    private static VaultMTP instance;
    private Logger pluginLogger;
    private String prefix;

    // 1. ДОБАВЛЯЕМ ПЕРЕМЕННУЮ ДЛЯ ХРАНЕНИЯ ПРОВАЙДЕРА
    private Provider providerManager;

    @Override
    public void onEnable() {
        instance = this;
        pluginLogger = getLogger();

        String ymlPrefix = getDescription().getPrefix();
        this.prefix = (ymlPrefix != null && !ymlPrefix.isEmpty()) ? "[" + ymlPrefix + "] " : "[VaultMT] ";

        logInfo("Инициализация ядра VaultMT...");

        // 2. ИНИЦИАЛИЗИРУЕМ МЕНЕДЖЕР И ЗАПУСКАЕМ СЕТАП
        this.providerManager = new Provider();
        this.providerManager.setup();

        logInfo("VaultMT успешно запущен! Версия: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        logInfo("Выключение VaultMT...");
        logInfo("Плагин выключен.");
    }

    public void logInfo(String message) {
        getServer().getConsoleSender().sendMessage(prefix + "§a" + message);
    }

    public static VaultMTP getInstance() {
        return instance;
    }

    // 3. ДОБАВЛЯЕМ ГЕТТЕР, ЧТОБЫ PLAYERJOIN МОГ ПОЛУЧИТЬ ДОСТУП
    public Provider getProviderManager() {
        return providerManager;
    }
}