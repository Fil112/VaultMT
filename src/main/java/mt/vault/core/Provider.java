package mt.vault.core;

import mt.vault.api.EconomyProvider;
import mt.vault.api.VaultMT;
import org.bukkit.Bukkit;

public class Provider {

    // Здесь хранится активная система экономики (внутренняя или от другого плагина)
    private EconomyProvider activeProvider;

    /**
     * Метод инициализации. Вызывается при запуске плагина.
     */
    public void setup() {
        VaultMTP plugin = VaultMTP.getInstance();
        plugin.logInfo("Поиск доступных провайдеров экономики...");

        // Шаг 1: Проверяем наличие популярных плагинов
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            plugin.logInfo("Обнаружен EssentialsX! Подключаем адаптер...");
            // TODO: activeProvider = new EssentialsAdapter();
        }
        // Шаг 2: Если ничего не найдено, запускаем свой Standalone режим
        else {
            plugin.logInfo("Сторонних плагинов не найдено. Запуск встроенной базы данных SQLite...");

            // Получаем стартовый баланс из конфига (по умолчанию 100.0)
            double startBal = plugin.getConfig().getDouble("economy.start-balance", 100.0);

            // Инициализируем провайдер
            EconomyProvider sqlite = new SQLiteProvider(plugin.getDataFolder(), startBal);

            // Устанавливаем его как активный
            setProvider(sqlite);
        }
    }

    /**
     * Позволяет другим твоим плагинам (или сторонним разработчикам)
     * принудительно установить свой провайдер.
     */
    public void setProvider(EconomyProvider provider) {
        this.activeProvider = provider;
        VaultMT.setProvider(provider);
        VaultMTP.getInstance().logInfo("Установлен новый провайдер экономики: " + provider.getName());
    }

    public void reload() {
        if (activeProvider != null) {
            activeProvider.close(); // Закрываем старую базу
        }
        setup(); // Заново читаем конфиг и подключаем новую
    }

    /**
     * Возвращает текущую активную экономику.
     * Если она еще не загружена, может вернуть null (нужно будет проверять).
     */
    public EconomyProvider get() {
        return activeProvider;
    }
}