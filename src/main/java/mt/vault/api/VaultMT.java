package mt.vault.api;

public class VaultMT {

    // Статичная переменная, хранящая текущую активную экономику
    private static EconomyProvider provider = null;

    /**
     * Главный метод для сторонних плагинов.
     * Пример использования в другом плагине:
     * EconomyProvider economy = VaultMT.getProvider();
     */
    public static EconomyProvider getProvider() {
        return provider;
    }

    /**
     * Внутренний метод для установки провайдера.
     * Его будет вызывать наше ядро при старте сервера.
     */
    public static void setProvider(EconomyProvider economyProvider) {
        provider = economyProvider;
    }

    /**
     * Удобный метод для проверки, загрузилась ли экономика.
     */
    public static boolean hasProvider() {
        return provider != null;
    }
}