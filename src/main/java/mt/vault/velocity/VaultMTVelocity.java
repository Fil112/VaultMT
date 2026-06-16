package mt.vault.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import mt.vault.core.VaultPlatform;
import org.slf4j.Logger;
import java.nio.file.Path;

@Plugin(id = "vaultmt", name = "VaultMT", version = "1.0.0", authors = {"MT Studio"})
public class VaultMTVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private VaultPlatform platform;

    // Уникальный идентификатор канала
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("vaultmt:main");

    @Inject
    public VaultMTVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.platform = new VelocityPlatform(this, server, logger, dataDirectory);
        platform.info("Инициализация VaultMT на Velocity...");

        server.getChannelRegistrar().register(CHANNEL);
        platform.info("Канал vaultmt:main успешно зарегистрирован!");
    }
}