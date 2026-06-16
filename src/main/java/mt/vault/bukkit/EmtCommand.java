package mt.vault.bukkit;

import mt.vault.core.SQLiteProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;
import java.util.List;

public class EmtCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = VaultMTBukkit.getInstance().getLangManager();

        if (args.length == 0) {
            sendHelp(sender, lang);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(lang.getMessage("no-permission"));
                    return true;
                }

                VaultMTBukkit.getInstance().reloadConfig();
                lang.loadMessages();
                VaultMTBukkit.getInstance().getProviderManager().reload();

                sender.sendMessage(lang.getMessage("reload-success"));
                break;

            case "help":
                sendHelp(sender, lang);
                break;

            case "version":
                sender.sendMessage("§7=====================================");
                sender.sendMessage("§fPlugin: §fVaultMT (§eEMT§f)");
                sender.sendMessage("§fVersion: §f1.0.0");
                sender.sendMessage("§7=====================================");
                break;

            case "give":
            case "set":
            case "take":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(lang.getMessage("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(lang.getMessage("usage-message").replace("{command}", subCommand));
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount < 0) {
                        sender.sendMessage(lang.getMessage("no-amount"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang.getMessage("not-a-number").replace("{value}", args[2]));
                    return true;
                }

                OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(args[1]);

                if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
                    sender.sendMessage(lang.getMessage("player-never-played").replace("{player}", args[1]));
                    return true;
                }

                EconomyProvider provider = mt.vault.api.VaultMT.getProvider();
                if (provider == null) {
                    sender.sendMessage(lang.getMessage("error-provider"));
                    return true;
                }

                TransactionResult result = null;
                String targetName = targetOffline.getName();

                if (subCommand.equals("give")) {
                    result = provider.deposit(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage(lang.getMessage("give-sender")
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{player}", targetName));
                    }
                } else if (subCommand.equals("set")) {
                    result = provider.setBalance(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage(lang.getMessage("set-sender")
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{player}", targetName));
                    }
                } else if (subCommand.equals("take")) {
                    result = provider.withdraw(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage(lang.getMessage("take-sender")
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{player}", targetName));
                    } else if (result.status() == mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                        sender.sendMessage(lang.getMessage("take-insufficient")
                                .replace("{balance}", String.format("%.2f", provider.getBalance(targetOffline.getUniqueId()))));
                        return true;
                    }
                }

                if (result != null && !result.isSuccess() && result.status() != mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                    sender.sendMessage(lang.getMessage("transaction-error").replace("{error}", result.errorMessage()));
                    return true;
                }

                if (result != null && result.isSuccess() && targetOffline.isOnline() && targetOffline.getPlayer() != null) {
                    Player onlineTarget = targetOffline.getPlayer();
                    if (subCommand.equals("give")) {
                        onlineTarget.sendMessage(lang.getMessage("give-target").replace("{amount}", String.valueOf(amount)));
                    } else if (subCommand.equals("set")) {
                        onlineTarget.sendMessage(lang.getMessage("set-target").replace("{amount}", String.valueOf(amount)));
                    } else if (subCommand.equals("take")) {
                        onlineTarget.sendMessage(lang.getMessage("take-target").replace("{amount}", String.valueOf(amount)));
                    }
                }
                break;

            case "stats":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(lang.getMessage("no-permission"));
                    return true;
                }

                EconomyProvider statsProvider = mt.vault.api.VaultMT.getProvider();
                if (statsProvider == null) {
                    sender.sendMessage(lang.getMessage("error-provider"));
                    return true;
                }

                sender.sendMessage(lang.getMessage("stats-loading"));

                PlatformUtil.runAsync(VaultMTBukkit.getInstance(), () -> {
                    double totalMoney = 0;
                    double topBalance = -1;
                    String richestPlayer = "Никто";
                    int accountCount = 0;

                    for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                        if (statsProvider.hasAccount(op.getUniqueId())) {
                            double bal = statsProvider.getBalance(op.getUniqueId());
                            totalMoney += bal;
                            accountCount++;

                            if (bal > topBalance) {
                                topBalance = bal;
                                richestPlayer = op.getName() != null ? op.getName() : "Неизвестно";
                            }
                        }
                    }

                    final double finalTotal = totalMoney;
                    final String finalRichest = richestPlayer;
                    final double finalTopBal = topBalance;
                    final int finalCount = accountCount;

                    PlatformUtil.runSync(VaultMTBukkit.getInstance(), () -> {
                        sender.sendMessage(lang.getMessage("stats-title"));
                        sender.sendMessage(lang.getMessage("stats-accounts").replace("{count}", String.valueOf(finalCount)));
                        sender.sendMessage(lang.getMessage("stats-total").replace("{total}", String.format("%.2f", finalTotal)));
                        if (finalCount > 0) {
                            sender.sendMessage(lang.getMessage("stats-richest")
                                    .replace("{player}", finalRichest)
                                    .replace("{balance}", String.format("%.2f", finalTopBal)));
                        }
                        sender.sendMessage(lang.getMessage("stats-footer"));
                    });
                });
                break;

            case "bal":
            case "balance":
                OfflinePlayer target = null;

                if (args.length > 1) {
                    target = Bukkit.getOfflinePlayer(args[1]);
                } else if (sender instanceof Player) {
                    target = (OfflinePlayer) sender;
                } else {
                    sender.sendMessage(lang.getMessage("console-needs-player"));
                    return true;
                }

                EconomyProvider balProvider = mt.vault.api.VaultMT.getProvider();
                if (balProvider == null) {
                    sender.sendMessage(lang.getMessage("error-provider"));
                    return true;
                }

                double bal = balProvider.getBalance(target.getUniqueId());
                sender.sendMessage(lang.getMessage("balance-check")
                        .replace("{player}", target.getName() != null ? target.getName() : args[1])
                        .replace("{balance}", String.format("%.2f", bal)));
                break;

            case "pay":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(lang.getMessage("no-console"));
                    return true;
                }

                Player pSender = (Player) sender;

                if (args.length < 3) {
                    pSender.sendMessage(lang.getMessage("pay-usage"));
                    return true;
                }

                double payAmount;
                try {
                    payAmount = Double.parseDouble(args[2]);
                    if (payAmount <= 0) {
                        pSender.sendMessage(lang.getMessage("pay-zero"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    pSender.sendMessage(lang.getMessage("not-a-number").replace("{value}", args[2]));
                    return true;
                }

                OfflinePlayer targetP = Bukkit.getOfflinePlayer(args[1]);

                if (!targetP.hasPlayedBefore() && !targetP.isOnline()) {
                    pSender.sendMessage(lang.getMessage("player-never-played").replace("{player}", args[1]));
                    return true;
                }

                if (pSender.getUniqueId().equals(targetP.getUniqueId())) {
                    pSender.sendMessage(lang.getMessage("pay-self"));
                    return true;
                }

                EconomyProvider payProvider = mt.vault.api.VaultMT.getProvider();
                if (payProvider == null) {
                    pSender.sendMessage(lang.getMessage("error-provider"));
                    return true;
                }

                double feeRate = VaultMTBukkit.getInstance().getConfig().getDouble("settings.transfer-fee",
                        VaultMTBukkit.getInstance().getConfig().getDouble("transfer-fee", 0.0));

                double fee = payAmount * feeRate;
                double totalDeduction = payAmount + fee;

                if (payProvider.getBalance(pSender.getUniqueId()) < totalDeduction) {
                    if (fee > 0) {
                        pSender.sendMessage(lang.getMessage("pay-insufficient-fee")
                                .replace("{amount}", String.valueOf(payAmount))
                                .replace("{fee}", String.valueOf(feeRate * 100))
                                .replace("{total}", String.format("%.2f", totalDeduction)));
                    } else {
                        pSender.sendMessage(lang.getMessage("pay-insufficient"));
                    }
                    return true;
                }

                TransactionResult withdrawResult = payProvider.withdraw(pSender.getUniqueId(), totalDeduction);

                if (withdrawResult.isSuccess()) {
                    TransactionResult depositResult = payProvider.deposit(targetP.getUniqueId(), payAmount);

                    if (depositResult.isSuccess()) {
                        if (fee > 0) {
                            pSender.sendMessage(lang.getMessage("pay-success-fee")
                                    .replace("{amount}", String.valueOf(payAmount))
                                    .replace("{player}", targetP.getName())
                                    .replace("{fee}", String.format("%.2f", fee)));
                        } else {
                            pSender.sendMessage(lang.getMessage("pay-success")
                                    .replace("{amount}", String.valueOf(payAmount))
                                    .replace("{player}", targetP.getName()));
                        }

                        if (targetP.isOnline() && targetP.getPlayer() != null) {
                            targetP.getPlayer().sendMessage(lang.getMessage("pay-received")
                                    .replace("{player}", pSender.getName())
                                    .replace("{amount}", String.valueOf(payAmount)));
                        }
                    } else {
                        payProvider.deposit(pSender.getUniqueId(), totalDeduction);
                        pSender.sendMessage(lang.getMessage("pay-refunded"));
                        VaultMTBukkit.getInstance().getLogger().warning("Сбой транзакции /emt pay: " + depositResult.errorMessage());
                    }
                } else {
                    pSender.sendMessage(lang.getMessage("pay-fail").replace("{error}", withdrawResult.errorMessage()));
                }

                if (payProvider instanceof SQLiteProvider) {
                    SQLiteProvider sqlProvider = (SQLiteProvider) payProvider;
                    String currency = VaultMTBukkit.getInstance().getConfig().getString("economy.currency.symbol", "mt");

                    sqlProvider.addLog(pSender.getUniqueId(), pSender.getName(), "Перевод игроку " + targetP.getName(), -totalDeduction, currency);
                    sqlProvider.addLog(targetP.getUniqueId(), targetP.getName(), "Получено от " + pSender.getName(), payAmount, currency);
                }
                break;

            case "log":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(lang.getMessage("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(lang.getMessage("log-usage"));
                    return true;
                }

                OfflinePlayer logTarget = Bukkit.getOfflinePlayer(args[1]);
                if (!logTarget.hasPlayedBefore() && !logTarget.isOnline()) {
                    sender.sendMessage(lang.getMessage("player-never-played").replace("{player}", args[1]));
                    return true;
                }

                int page = 1;
                if (args.length >= 3) {
                    try {
                        page = Integer.parseInt(args[2]);
                        if (page < 1) page = 1;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(lang.getMessage("page-not-number"));
                        return true;
                    }
                }

                final int finalPage = page;
                EconomyProvider prov = mt.vault.api.VaultMT.getProvider();

                if (prov instanceof SQLiteProvider) {
                    SQLiteProvider sqlProvider = (SQLiteProvider) prov;

                    PlatformUtil.runAsync(VaultMTBukkit.getInstance(), () -> {
                        List<String> logs = sqlProvider.getLogs(logTarget.getUniqueId(), finalPage);

                        PlatformUtil.runSync(VaultMTBukkit.getInstance(), () -> {
                            sender.sendMessage(lang.getMessage("log-header")
                                    .replace("{player}", logTarget.getName() != null ? logTarget.getName() : args[1])
                                    .replace("{page}", String.valueOf(finalPage)));
                            if (logs.isEmpty()) {
                                sender.sendMessage(lang.getMessage("log-empty"));
                            } else {
                                for (String logStr : logs) {
                                    sender.sendMessage(logStr);
                                }
                            }
                        });
                    });
                } else {
                    sender.sendMessage(lang.getMessage("log-only-db"));
                }
                break;

            default:
                sender.sendMessage(lang.getMessage("unknown-command"));
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender, LanguageManager lang) {
        sender.sendMessage(lang.getMessage("help-header"));
        sender.sendMessage(lang.getMessage("help-balance"));
        sender.sendMessage(lang.getMessage("help-pay"));
        sender.sendMessage(lang.getMessage("help-stats"));
        if (sender.hasPermission("vaultmt.admin")) {
            sender.sendMessage(lang.getMessage("help-give"));
            sender.sendMessage(lang.getMessage("help-take"));
            sender.sendMessage(lang.getMessage("help-set"));
            sender.sendMessage(lang.getMessage("help-log"));
            sender.sendMessage(lang.getMessage("help-version"));
            sender.sendMessage(lang.getMessage("help-reload"));
        }
        sender.sendMessage(lang.getMessage("help-footer"));
    }
}