package mt.vault.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final VaultMTP plugin;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(VaultMTP plugin) {
        this.plugin = plugin;
        init(); // Запускает создание папок, копирование файлов и их загрузку
    }

    /**
     * Первичная настройка: создание папки и надежное копирование файлов из JAR.
     */
    private void init() {
        File langFolder = new File(plugin.getDataFolder(), "languages");

        // Создаем папку, если её нет
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] defaultLangs = {"ru_RU.yml", "en_US.yml"};

        for (String fileName : defaultLangs) {
            File langFile = new File(langFolder, fileName);

            // Ищем поток файла: сначала стандартным методом Bukkit, затем системным загрузчиком Java
            InputStream in = plugin.getResource("languages/" + fileName);
            if (in == null) {
                in = getClass().getResourceAsStream("/languages/" + fileName);
            }

            if (in != null) {
                try {
                    // Используем REPLACE_EXISTING, чтобы перезаписать файл, если он пустой или битый
                    Files.copy(in, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    plugin.getLogger().severe("Не удалось скопировать файл: " + fileName);
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (Exception ignored) {}
                }
            } else {
                // Если файла реально нет в архиве, пишем предупреждение, но плагин не крашим
                if (!langFile.exists()) {
                    plugin.getLogger().warning("Ресурс не найден в JAR: languages/" + fileName);
                }
            }
        }

        // После проверки/создания файлов загружаем их в память
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

        // Если указанный в конфиге файл не существует, откатываемся к стандарту
        if (!langFile.exists()) {
            plugin.getLogger().warning("Язык " + langName + " не найден! Использую ru_RU.yml");
            langFile = new File(plugin.getDataFolder(), "languages/ru_RU.yml");
            fileName = "languages/ru_RU.yml";
        }

        // Если даже стандартный файл не найден, прерываем логику
        if (!langFile.exists()) {
            plugin.getLogger().severe("Критическая ошибка: Ни один языковой файл не найден!");
            return;
        }

        // Загрузка файла с диска
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Подгружаем дефолтные значения из jar на случай, если админ удалил строки вручную
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) {
            defaultStream = getClass().getResourceAsStream("/" + fileName);
        }

        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);

            try {
                defaultStream.close();
            } catch (Exception ignored) {}
        }

        // Переносим все сообщения в хэшмапу и сразу заменяем цветовые коды '&' на '§'
        if (langConfig.getConfigurationSection("messages") != null) {
            for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                String message = langConfig.getString("messages." + key);
                if (message != null) {
                    messageCache.put(key, message.replace('&', '§'));
                }
            }
        } else {
            plugin.getLogger().warning("В файле " + langFile.getName() + " не найдена секция 'messages:'! Сообщения не загружены.");
        }
    }

    /**
     * Получает строку по ключу. Если ключ отсутствует, возвращает заглушку с ошибкой.
     */
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§c[Missing message: " + key + "]");
    }
}