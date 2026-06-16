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

        // Получаем нашего менеджера и активную экономику
        Provider providerManager = VaultMTBukkit.getInstance().getProviderManager();
        if (providerManager == null) return;

        EconomyProvider economy = providerManager.get();

        // Если провайдер экономики успешно загружен
        if (economy != null) {
            // Проверяем, есть ли у игрока счет по его уникальному ID (UUID)
            if (!economy.hasAccount(player.getUniqueId())) {
                // Если счета нет — создаем его
                economy.createAccount(player.getUniqueId());
                VaultMTBukkit.getInstance().logInfo("Создан новый счет для игрока: " + player.getName());
            }
        }
    }
}