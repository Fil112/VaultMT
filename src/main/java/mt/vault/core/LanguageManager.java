package mt.vault.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final VaultMTP plugin;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(VaultMTP plugin) {
        this.plugin = plugin;
        setupLanguageFolder();
        loadMessages();
    }

    /**
     * Загружает или перезагружает языковой файл из конфигурации.
     */
    public void loadMessages() {
        messageCache.clear();
        String langName = plugin.getConfig().getString("settings.language", "ru_RU");
        String fileName = "languages/" + langName + ".yml";
        File langFile = new File(plugin.getDataFolder(), "languages/" + langName + ".yml");

        // Если файла нет на диске, пытаемся вытащить его из ресурсов JAR
        if (!langFile.exists()) {
            plugin.getLogger().warning("Файл " + langName + ".yml не найден, пытаюсь извлечь из JAR...");
            // ВАЖНО: используем "/" в начале, чтобы искать от корня JAR
            InputStream is = plugin.getResource("languages/" + langName + ".yml");
            if (is != null) {
                plugin.saveResource("languages/" + langName + ".yml", false);
            } else {
                plugin.getLogger().severe("Файл " + langName + ".yml не найден внутри JAR!");
                return; // Прерываем, так как нет файла для загрузки
            }
        }

        // Загрузка
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Подгружаем дефолтные значения из jar на случай, если админ удалил строки вручную
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);
        }

        // Переносим все сообщения в хэшмапу и сразу заменяем цветовые коды '&' на '§'
        if (langConfig.getConfigurationSection("messages") != null) {
            for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                String message = langConfig.getString("messages." + key);
                if (message != null) {
                    messageCache.put(key, message.replace('&', '§'));
                }
            }
        }
    }

    /**
     * Создает папку languages и копирует туда дефолтные файлы, если их нет.
     */
    private void setupLanguageFolder() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) langFolder.mkdirs();

        String[] defaultLangs = {"ru_RU.yml", "en_US.yml"};

        for (String fileName : defaultLangs) {
            File langFile = new File(langFolder, fileName);
            if (!langFile.exists()) {
                try (InputStream in = plugin.getResource("languages/" + fileName)) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, langFile.toPath());
                    } else {
                        plugin.getLogger().warning("Ресурс не найден в JAR: languages/" + fileName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Не удалось скопировать файл: " + fileName);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Получает строку по ключу. Если ключ отсутствует, возвращает заглушку с ошибкой.
     */
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§c[Missing message: " + key + "]");
    }
}