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

        // 1. Сохраняем стандартный config.yml самым первым
        saveDefaultConfig();

        // Устанавливаем префикс для красивых логов
        String ymlPrefix = getDescription().getPrefix();
        this.prefix = (ymlPrefix != null && !ymlPrefix.isEmpty()) ? "[" + ymlPrefix + "] " : "[VaultMT] ";

        logInfo("Инициализация ядра VaultMT...");

        // 2. Инициализируем локализацию СРАЗУ ПОСЛЕ конфига, до баз данных и команд
        this.langManager = new LanguageManager(this);
        // Метод loadMessages() теперь автоматически вызывается внутри конструктора LanguageManager

        // 3. Подключаем базу данных (SQLite/MySQL) через твой Provider
        this.providerManager = new Provider();
        this.providerManager.setup();

        // 4. Регистрация команд
        if (getCommand("emt") != null) {
            getCommand("emt").setExecutor(new EmtCommand());
        }

        logInfo("VaultMT успешно запущен! Версия: " + getDescription().getVersion());

        // 5. Интеграция с PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EmtExpansion().register();
            logInfo("PlaceholderAPI найден! Плейсхолдер %vaultmt_balance% зарегистрирован.");
        } else {
            logInfo("PlaceholderAPI не найден. Плейсхолдеры отключены.");
        }
    }

    @Override
    public void onDisable() {
        logInfo("Выключение VaultMT...");
        // В будущем здесь можно добавить providerManager.close(); для безопасного отключения БД
        logInfo("Плагин выключен.");
    }

    public void logInfo(String message) {
        getServer().getConsoleSender().sendMessage(prefix + "§a" + message);
    }

    // ==========================================
    // ГЕТТЕРЫ ДЛЯ ДОСТУПА ИЗ ДРУГИХ КЛАССОВ
    // ==========================================

    public static VaultMTP getInstance() {
        return instance;
    }

    public LanguageManager getLangManager() {
        return langManager;
    }

    public Provider getProviderManager() {
        return providerManager;
    }
}