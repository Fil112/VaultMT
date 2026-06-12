package mt.vault.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import mt.vault.api.EconomyProvider;
import mt.vault.api.VaultMT;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class EmtExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "vaultmt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Keylo";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.0.5";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        EconomyProvider provider = VaultMT.getProvider();
        if (provider == null) return "0.0";

        // %vaultmt_balance% — просто число
        if (params.equalsIgnoreCase("balance")) {
            return String.valueOf(provider.getBalance(player.getUniqueId()));
        }

        // %vaultmt_formatted% — баланс с символом валюты (из конфига)
        if (params.equalsIgnoreCase("formatted")) {
            double bal = provider.getBalance(player.getUniqueId());
            String format = VaultMTP.getInstance().getConfig().getString("economy.currency.format", "%.2f $");
            String symbol = VaultMTP.getInstance().getConfig().getString("economy.currency.symbol", "$");
            return String.format(format, bal, symbol);
        }

        // %vaultmt_fee% — текущая комиссия в процентах для инфо-панелей
        if (params.equalsIgnoreCase("fee")) {
            double feeRate = VaultMTP.getInstance().getConfig().getDouble("transfer-fee", 0.0);
            return (feeRate * 100) + "%";
        }

        return null;
    }
}