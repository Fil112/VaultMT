package mt.vault.sponge;

import com.google.inject.Inject;
import mt.vault.core.Provider;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;

// В API 17 указываем только ID, всё остальное сервер берет из sponge_plugins.json
@Plugin("vaultmt")
public class VaultMTSponge {

    @Inject
    private Logger logger;

    // Sponge автоматически дает путь к папке config/vaultmt/
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    // Автоматически получаем контейнер плагина
    @Inject
    private PluginContainer container;

    private static VaultMTSponge instance;
    private Provider providerManager;
    private SpongePlatform platform;

    private static LanguageManager languageManager; // Добавляем поле

    @Listener
    public void onServerStart(StartedEngineEvent<Server> event) {
        instance = this;
        logger.info("Инициализация ядра VaultMT на платформе SpongeNeo (1.21.x)...");

        // 1. Инициализация платформы (создаст папку и конфиги)
        this.platform = new SpongePlatform(this, logger, configDir);
        languageManager = new LanguageManager(platform);

        this.providerManager = new Provider();

        // 2. Инициализация провайдера экономики (создаст базу данных)
        this.providerManager = new Provider();
        this.providerManager.setup(platform);
        Sponge.eventManager().registerListeners(container, new SpongeJoinListener());

        logger.info("VaultMT успешно запущен на Sponge!");
    }

    // Регистрация команд всегда происходит в этом отдельном ивенте
    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event) {
        EmtCommandSponge.register(event, container);
        logger.info("Команды VaultMT успешно зарегистрированы!");
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<Server> event) {
        logger.info("Выключение VaultMT...");
        if (providerManager != null) {
            // Вызываем reload(), который внутри закроет коннект к БД
            providerManager.reload();
        }
    }

    public static VaultMTSponge getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public SpongePlatform getPlatform() {
        return platform;
    }
    public static LanguageManager getLang() {return languageManager;}
}