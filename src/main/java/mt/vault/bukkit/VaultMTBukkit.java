package mt.vault.bukkit;

import mt.vault.bridge.BridgeGateway;
import mt.vault.bridge.BridgeProcessor;
import mt.vault.core.Provider;
import mt.vault.core.VaultPlatform; // Добавлен импорт платформы
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class VaultMTBukkit extends JavaPlugin {

    private static VaultMTBukkit instance;
    private Logger pluginLogger;
    private String prefix;
    private LanguageManager langManager;
    private Provider providerManager;
    private VaultPlatform platform; // Сохраняем платформу для переиспользования

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

        // 3. Подключаем платформу и базу данных
        this.platform = new BukkitPlatform(this); // Инициализируем BukkitPlatform
        this.providerManager = new Provider();
        this.providerManager.setup(platform);     // Передаем платформу в Provider

        // Создаем обработчик мостов, передаем ему ТУ ЖЕ платформу
        BridgeProcessor processor = new BridgeProcessor(this.platform, this.providerManager);
        BridgeGateway.init(processor);

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
        if (providerManager != null) {
            providerManager.reload(); // Безопасное отключение провайдера/БД
        }
        logInfo("Плагин выключен.");
    }

    public void logInfo(String message) {
        getServer().getConsoleSender().sendMessage(prefix + "§a" + message);
    }

    // ==========================================
    // ГЕТТЕРЫ ДЛЯ ДОСТУПА ИЗ ДРУГИХ КЛАССОВ
    // ==========================================

    public static VaultMTBukkit getInstance() {
        return instance;
    }

    public LanguageManager getLangManager() {
        return langManager;
    }

    public Provider getProviderManager() {
        return providerManager;
    }

    public VaultPlatform getPlatform() {
        return platform;
    }
}