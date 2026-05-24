package mt.vault.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import mt.vault.api.EconomyProvider;
import mt.vault.api.VaultMT;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class EmtExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "vaultmt"; // Это значит, что плейсхолдеры будут начинаться с %vaultmt_
    }

    @Override
    public @NotNull String getAuthor() {
        return "Keylo";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.0.3-A";
    }

    // ВАЖНО: Нельзя заставлять плагин зависать в этом методе (не делай долгих запросов к БД)
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // PAPI может иногда отправлять null, если плейсхолдер вызывается сервером, а не игроком
        if (player == null) return "";

        // Обрабатываем %vaultmt_balance%
        if (params.equalsIgnoreCase("balance")) {
            EconomyProvider provider = VaultMT.getProvider();
            if (provider != null) {
                // Вызываем метод из твоего API
                double balance = provider.getBalance(player.getUniqueId());
                return String.valueOf(balance);
            }
            return "0.0";
        }

        return null; // Если запросили неизвестный плейсхолдер
    }
}