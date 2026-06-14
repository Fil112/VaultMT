package mt.vault.core;

import mt.vault.api.EconomyProvider;
import mt.vault.api.VaultMT;

public class Provider {

    private EconomyProvider activeProvider;
    private VaultPlatform platform;

    // Теперь мы передаем платформу снаружи!
    public void setup(VaultPlatform platform) {
        this.platform = platform;
        platform.info("Запуск системы экономики...");

        // Пока что для Sponge мы жестко запускаем SQLite.
        // Адаптер для Essentials мы добавим позже отдельно для Bukkit.
        platform.info("Запуск встроенной базы данных SQLite...");
        SQLiteProvider sqlite = new SQLiteProvider(platform);
        sqlite.connect();

        setProvider(sqlite);
    }

    public void setProvider(EconomyProvider provider) {
        this.activeProvider = provider;
        VaultMT.setProvider(provider);
        if (platform != null) {
            platform.info("Установлен новый провайдер экономики: " + provider.getName());
        }
    }

    public void reload() {
        if (activeProvider != null) {
            activeProvider.close();
        }
        if (platform != null) {
            setup(platform);
        }
    }

    public EconomyProvider get() {
        return activeProvider;
    }
}