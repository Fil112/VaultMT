package mt.vault.bukkit;

import mt.vault.api.VaultMT;
import mt.vault.core.Provider;
import mt.vault.core.VaultPlatform;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class VaultMTBukkit extends JavaPlugin {

    private static VaultMTBukkit instance;
    private Logger pluginLogger;
    private String prefix;
    private LanguageManager langManager;
    private Provider providerManager;
    private VaultPlatform platform;

    @Override
    public void onEnable() {
        instance = this;
        pluginLogger = getLogger();

        // 1. Сохраняем стандартный config.yml самым первым
        saveDefaultConfig();

        // Устанавливаем префикс для красивых логов
        String ymlPrefix = getDescription().getPrefix();
        this.prefix = (ymlPrefix != null && !ymlPrefix.isEmpty()) ? "[" + ymlPrefix + "] " : "[VaultMT] ";

        logInfo("Инициализация независимого ядра экономики MT...");

        // 2. Инициализируем локализацию
        this.langManager = new LanguageManager(this);

        // 3. Подключаем платформу и базу данных
        this.platform = new BukkitPlatform(this);
        this.providerManager = new Provider();
        this.providerManager.setup(platform);

        // 4. Регистрация команд
        if (getCommand("emt") != null) {
            getCommand("emt").setExecutor(new EmtCommand());
        }

        // 5. Интеграция с PlaceholderAPI
        if (getConfig().getBoolean("api.enable-papi", true)) {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new EmtExpansion().register();
                logInfo("PlaceholderAPI найден! Плейсхолдеры зарегистрированы.");
            }
        }

        getServer().getPluginManager().registerEvents(new PlayerJoin(), this);

        logInfo("Движок VaultMT успешно запущен! Версия: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        logInfo("Выключение VaultMT...");
        if (VaultMT.hasProvider()) {
            VaultMT.getProvider().close();
        }

        if (providerManager != null) {
            providerManager.reload();
        }

        logInfo("Плагин выключен.");
    }

    public void logInfo(String message) {
        getServer().getConsoleSender().sendMessage(prefix + "§a" + message);
    }

    public static VaultMTBukkit getInstance() { return instance; }
    public LanguageManager getLangManager() { return langManager; }
    public Provider getProviderManager() { return providerManager; }
    public VaultPlatform getPlatform() { return platform; }
}