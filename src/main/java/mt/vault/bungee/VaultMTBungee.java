package mt.vault.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import mt.vault.core.Provider;
import mt.vault.core.VaultPlatform;

public class VaultMTBungee extends Plugin {

    private static VaultMTBungee instance;
    private VaultPlatform platform;
    private Provider providerManager;

    @Override
    public void onEnable() {
        instance = this;
        this.platform = new BungeePlatform(this, getLogger());

        platform.info("Инициализация VaultMT на BungeeCord/Waterfall...");

        // 1. Инициализируем базу данных (только MySQL)
        this.providerManager = new Provider();
        this.providerManager.setup(platform);

        // 2. Регистрация канала связи
        getProxy().registerChannel("vaultmt:main");

        // 3. Регистрация слушателя
        getProxy().getPluginManager().registerListener(this, new BungeeMessageListener());

        platform.info("Канал vaultmt:main и экономика успешно запущены!");
    }

    public static VaultMTBungee getInstance() {
        return instance;
    }
}