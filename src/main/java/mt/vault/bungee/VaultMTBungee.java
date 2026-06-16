package mt.vault.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import mt.vault.core.VaultPlatform;

public class VaultMTBungee extends Plugin {

    private static VaultMTBungee instance;
    private VaultPlatform platform;

    @Override
    public void onEnable() {
        instance = this;
        this.platform = new BungeePlatform(this, getLogger());

        platform.info("Инициализация VaultMT на BungeeCord/Waterfall...");

        // Регистрация канала связи
        getProxy().registerChannel("vaultmt:main");

        platform.info("Канал vaultmt:main успешно зарегистрирован!");
    }

    public static VaultMTBungee getInstance() {
        return instance;
    }
}