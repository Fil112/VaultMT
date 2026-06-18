package mt.vault.sponge;

import mt.vault.core.VaultPlatform;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final VaultPlatform platform;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(VaultPlatform platform) {
        this.platform = platform;
        init(); // Запускает создание папок, копирование файлов и их загрузку
    }

    private void init() {
        // Получаем корневую папку плагина через платформу
        File langFolder = new File(platform.getPluginFolder(), "languages");

        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] defaultLangs = {"ru_RU.yml", "en_US.yml"};

        for (String fileName : defaultLangs) {
            File langFile = new File(langFolder, fileName);
            // В Sponge ресурсы можно получать напрямую через ClassLoader
            InputStream in = getClass().getResourceAsStream("/languages/" + fileName);

            if (in != null) {
                try {
                    if (!langFile.exists()) {
                        Files.copy(in, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    platform.severe("Не удалось скопировать файл локализации: " + fileName);
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (Exception ignored) {}
                }
            } else {
                if (!langFile.exists()) {
                    platform.warning("Ресурс локализации не найден внутри JAR: languages/" + fileName);
                }
            }
        }

        loadMessages();
    }

    public void loadMessages() {
        messageCache.clear();

        // Читаем настройку языка напрямую через твою абстракцию платформы[cite: 10]
        String langName = platform.getConfigString("settings.language", "ru_RU");
        File langFile = new File(platform.getPluginFolder(), "languages/" + langName + ".yml");

        if (!langFile.exists()) {
            platform.warning("Языковой файл " + langName + ".yml не найден! Откат к ru_RU.yml");
            langFile = new File(platform.getPluginFolder(), "languages/ru_RU.yml");
            langName = "ru_RU";
        }

        // Используем YamlConfigurationLoader из Configurate
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(langFile.toPath())
                .build();

        try {
            ConfigurationNode langNode = loader.load();

            // 1. Подгружаем дефолтные строки прямо из JAR
            InputStream defaultStream = getClass().getResourceAsStream("/languages/" + langName + ".yml");
            if (defaultStream == null) {
                defaultStream = getClass().getResourceAsStream("/languages/ru_RU.yml");
            }

            if (defaultStream != null) {
                try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                    YamlConfigurationLoader defaultLoader = YamlConfigurationLoader.builder()
                            .source(() -> new BufferedReader(reader))
                            .build();
                    ConfigurationNode defaultNode = defaultLoader.load();
                    // Объединяем ноды (аналог setDefaults из Bukkit)
                    langNode.mergeFrom(defaultNode);
                }
            }

            // 2. Сохраняем все сообщения в кэш
            ConfigurationNode messagesNode = langNode.node("messages");
            if (!messagesNode.virtual()) {
                // Проходимся по всем ключам в секции "messages"
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : messagesNode.childrenMap().entrySet()) {
                    String key = entry.getKey().toString();
                    String message = entry.getValue().getString();
                    if (message != null) {
                        messageCache.put(key, message.replace('&', '§'));
                    }
                }
            } else {
                platform.severe("Критическая ошибка: В файле " + langFile.getName() + " не найдена секция 'messages:'!");
            }

        } catch (Exception e) {
            platform.severe("Ошибка при парсинге конфигурации языка: " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§c[Missing message: " + key + "]");
    }
}