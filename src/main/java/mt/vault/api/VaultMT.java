package mt.vault.api;

/**
 * Официальный API экосистемы MT.
 * Через этот класс сторонние плагины могут безопасно взаимодействовать с экономикой.
 *
 * Пример использования:
 * EconomyProvider economy = VaultMT.getProvider();
 * if (economy != null) { economy.deposit(uuid, 100.0); }
 */
public class VaultMT {

    // Статичная переменная, хранящая активный движок экономики
    private static EconomyProvider provider = null;

    /**
     * Получить текущий провайдер экономики.
     *
     * @return EconomyProvider или null, если экономика еще не инициализирована.
     */
    public static EconomyProvider getProvider() {
        return provider;
    }

    /**
     * Проверяет, загружена ли экономика на сервере.
     *
     * @return true, если API готово к работе.
     */
    public static boolean hasProvider() {
        return provider != null;
    }

    /**
     * Внутренний метод для ядра MT.
     * Сторонние плагины НЕ должны вызывать этот метод!
     *
     * @param economyProvider Имплементация провайдера баз данных
     */
    public static void setProvider(EconomyProvider economyProvider) {
        provider = economyProvider;
    }
}