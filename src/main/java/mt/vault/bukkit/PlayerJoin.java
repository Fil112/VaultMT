package mt.vault.bukkit;

import mt.vault.api.EconomyProvider;
import mt.vault.core.Provider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Provider providerManager = VaultMTBukkit.getInstance().getProviderManager();
        if (providerManager == null) return;

        EconomyProvider economy = providerManager.get();

        if (economy != null) {
            if (!economy.hasAccount(LegacyAdapter.getUUID(player))) {
                economy.createAccount(LegacyAdapter.getUUID(player));
                VaultMTBukkit.getInstance().logInfo("Создан новый счет для игрока: " + player.getName());
            }
        }
    }
}