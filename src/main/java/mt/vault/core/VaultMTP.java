package mt.vault.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class VaultMTP extends JavaPlugin {

    private static VaultMTP instance;
    private Logger pluginLogger;
    private String prefix;
    private LanguageManager langManager;
    private Provider providerManager;

    @Override
    public void onEnable() {
        instance = this;
        pluginLogger = getLogger();

        // Сохраняем стандартный config.yml, если его нет
        saveDefaultConfig();

        // 1. Инициализируем и загружаем языки ПОСЛЕ сохранения дефолтного конфига
        this.langManager = new LanguageManager(this);
        this.langManager.loadMessages();

        String ymlPrefix = getDescription().getPrefix();
        this.prefix = (ymlPrefix != null && !ymlPrefix.isEmpty()) ? "[" + ymlPrefix + "] " : "[VaultMT] ";

        logInfo("Инициализация ядра VaultMT...");

        this.providerManager = new Provider();
        this.providerManager.setup();

        if (getCommand("emt") != null) {
            getCommand("emt").setExecutor(new EmtCommand());
        }

        logInfo("VaultMT успешно запущен! Версия: " + getDescription().getVersion());

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EmtExpansion().register();
            logInfo("PlaceholderAPI найден! Плейсхолдер %vaultmt_balance% зарегистрирован.");
        } else {
            logInfo("PlaceholderAPI не найден. Плейсхолдеры отключены.");
        }
    }

    // Геттер для получения менеджера языков из других классов
    public LanguageManager getLangManager() {
        return langManager;
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