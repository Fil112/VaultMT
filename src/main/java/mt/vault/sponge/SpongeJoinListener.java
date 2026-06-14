package mt.vault.sponge;

import mt.vault.api.EconomyProvider;
import mt.vault.api.VaultMT;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class SpongeJoinListener {

    @Listener
    public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();
        EconomyProvider economy = VaultMT.getProvider();

        if (economy != null) {
            if (!economy.hasAccount(player.uniqueId())) {
                economy.createAccount(player.uniqueId());
                VaultMTSponge.getInstance().getLogger().info("Создан новый счет для игрока: " + player.name());
            }
        }
    }
}