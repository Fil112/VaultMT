package mt.vault.bukkit;

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

    private final VaultMTBukkit plugin;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(VaultMTBukkit plugin) {
        this.plugin = plugin;
        init(); // Запускает создание папок, копирование файлов и их загрузку
    }

    private void init() {
        File langFolder = new File(plugin.getDataFolder(), "languages");

        // Создаем папку, если её нет
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] defaultLangs = {"ru_RU.yml", "en_US.yml"};

        for (String fileName : defaultLangs) {
            File langFile = new File(langFolder, fileName);

            // Используем встроенный метод Bukkit для получения ресурсов
            InputStream in = plugin.getResource("languages/" + fileName);

            if (in == null) {
                in = getClass().getResourceAsStream("/languages/" + fileName);
            }

            if (in != null) {
                try {
                    // Копируем файл только если его нет на диске, чтобы не затирать изменения админа
                    if (!langFile.exists()) {
                        Files.copy(in, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Не удалось скопировать файл локализации: " + fileName);
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (Exception ignored) {}
                }
            } else {
                if (!langFile.exists()) {
                    plugin.getLogger().warning("Ресурс локализации не найден внутри JAR: languages/" + fileName);
                }
            }
        }

        // После проверки/создания файлов загружаем их в память
        loadMessages();
    }

    public void loadMessages() {
        messageCache.clear();

        // Получаем язык из конфигурации Bukkit
        String langName = plugin.getConfig().getString("settings.language", "ru_RU");
        File langFile = new File(plugin.getDataFolder(), "languages/" + langName + ".yml");

        // Если указанный в конфиге файл отсутствует на диске, откатываемся к дефолту
        if (!langFile.exists()) {
            plugin.getLogger().warning("Языковой файл " + langName + ".yml не найден! Откат к ru_RU.yml");
            langFile = new File(plugin.getDataFolder(), "languages/ru_RU.yml");
            langName = "ru_RU";
        }

        // Загружаем файл с диска
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        // 1. ШАГ: Сначала подгружаем дефолтные строки прямо из JAR (гарантия защиты от missing keys)
        String internalResourcePath = "languages/" + langName + ".yml";
        InputStream defaultStream = plugin.getResource(internalResourcePath);
        if (defaultStream == null) {
            // Если для кастомного языка нет ресурса в JAR, берем за основу внутренний ru_RU.yml
            defaultStream = plugin.getResource("languages/ru_RU.yml");
        }

        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                langConfig.setDefaults(defaultConfig); // Устанавливаем резервные значения
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось прочитать дефолтные сообщения локализации из JAR.");
            }
        }

        // 2. ШАГ: Сохраняем все итоговые сообщения в кэш
        if (langConfig.getConfigurationSection("messages") != null) {
            for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
                // getString автоматически подтянет значение из defaultStream, если ключа нет в файле
                String message = langConfig.getString("messages." + key);
                if (message != null) {
                    // Сразу транслируем цветовые коды Bukkit
                    messageCache.put(key, message.replace('&', '§'));
                }
            }
        } else {
            plugin.getLogger().severe("Критическая ошибка: В файле " + langFile.getName() + " не найдена секция 'messages:'!");
        }
    }

    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "§c[Missing message: " + key + "]");
    }
}