package mt.vault.bukkit; // Замени на свой пакет

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
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                VaultMTP.getInstance().reloadConfig();
                VaultMTP.getInstance().getLangManager().loadMessages();
                VaultMTP.getInstance().getProviderManager().reload();

                sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("reload-success"));
                break;

            case "help":
                sendHelp(sender);
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
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 3) {
                    String usage = VaultMTP.getInstance().getLangManager().getMessage("usage-message")
                            .replace("{command}", subCommand);
                    sender.sendMessage(usage);
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount < 0) {
                        sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-amount"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cОшибка: §e" + args[2] + " §cне является числом!");
                    return true;
                }

                org.bukkit.OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(args[1]);

                if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!");
                    return true;
                }

                EconomyProvider provider = mt.vault.api.VaultMT.getProvider();
                if (provider == null) {
                    sender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("error-provider"));
                    return true;
                }

                TransactionResult result = null;

                if (subCommand.equals("give")) {
                    result = provider.deposit(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage("§aВы выдали §e" + amount + "$ §fигроку §b" + targetOffline.getName());
                    }
                } else if (subCommand.equals("set")) {
                    result = provider.setBalance(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage("§aВы установили баланс §e" + amount + "$ §fигроку §b" + targetOffline.getName());
                    }
                } else if (subCommand.equals("take")) {
                    result = provider.withdraw(targetOffline.getUniqueId(), amount);
                    if (result.isSuccess()) {
                        sender.sendMessage("§aВы забрали §e" + amount + "$ §fу игрока §b" + targetOffline.getName());
                    } else if (result.status() == mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                        sender.sendMessage("§cУ игрока недостаточно средств! Текущий баланс: §e" + provider.getBalance(targetOffline.getUniqueId()) + "$");
                        return true;
                    }
                }

                if (result != null && !result.isSuccess() && result.status() != mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                    sender.sendMessage("§cПроизошла ошибка при операции: " + result.errorMessage());
                    return true;
                }

                if (result != null && result.isSuccess() && targetOffline.isOnline() && targetOffline.getPlayer() != null) {
                    Player onlineTarget = targetOffline.getPlayer();
                    if (subCommand.equals("give")) {
                        onlineTarget.sendMessage("§aВам выдано §e" + amount + "$");
                    } else if (subCommand.equals("set")) {
                        onlineTarget.sendMessage("§6Ваш баланс административно установлен на §e" + amount + "$");
                    } else if (subCommand.equals("take")) {
                        onlineTarget.sendMessage("§cАдминистратор изъял у вас §e" + amount + "$");
                    }
                }
                break;

            case "stats":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                EconomyProvider statsProvider = mt.vault.api.VaultMT.getProvider();
                if (statsProvider == null) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("error-provider"));
                    return true;
                }

                sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("stats-loading"));

                PlatformUtil.runAsync(VaultMTP.getInstance(), () -> {
                    double totalMoney = 0;
                    double topBalance = -1;
                    String richestPlayer = "Никто";
                    int accountCount = 0;

                    for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                        if (statsProvider.hasAccount(op.getUniqueId())) {
                            double bal = statsProvider.getBalance(op.getUniqueId());
                            totalMoney += bal;
                            accountCount++;

                            if (bal > topBalance) {
                                topBalance = bal;
                                richestPlayer = op.getName() != null ? op.getName() : "Неизвестно (" + op.getUniqueId() + ")";
                            }
                        }
                    }

                    final double finalTotal = totalMoney;
                    final String finalRichest = richestPlayer;
                    final double finalTopBal = topBalance;
                    final int finalCount = accountCount;

                    PlatformUtil.runSync(VaultMTP.getInstance(), () -> {
                        sender.sendMessage("§7================ §eСтатистика EMT §7================");
                        sender.sendMessage("§fАктивных счетов: §e" + finalCount);
                        sender.sendMessage("§fВсего монет в обороте: §e" + String.format("%.2f", finalTotal) + "$");
                        if (finalCount > 0) {
                            sender.sendMessage("§fСамый богатый игрок: §b" + finalRichest + " §f(§e" + String.format("%.2f", finalTopBal) + "$§f)");
                        }
                        sender.sendMessage("§7==================================================");
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
                    sender.sendMessage("§cКонсоль должна указывать ник игрока: /emt balance <игрок>");
                    return true;
                }

                EconomyProvider balProvider = mt.vault.api.VaultMT.getProvider();
                if (balProvider == null) {
                    sender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    return true;
                }

                double bal = balProvider.getBalance(target.getUniqueId());
                sender.sendMessage("§eБаланс §b" + target.getName() + "§e: §a" + String.format("%.2f", bal) + "$");
                break;

            case "pay":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-console"));
                    return true;
                }

                Player pSender = (Player) sender;

                if (args.length < 3) {
                    pSender.sendMessage("§cИспользование: /emt pay <игрок> <сумма>");
                    return true;
                }

                double payAmount;
                try {
                    payAmount = Double.parseDouble(args[2]);
                    if (payAmount <= 0) {
                        pSender.sendMessage("§cСумма перевода должна быть больше нуля!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    pSender.sendMessage("§cОшибка: §e" + args[2] + " §cне является числом!");
                    return true;
                }

                OfflinePlayer targetP = Bukkit.getOfflinePlayer(args[1]);

                if (!targetP.hasPlayedBefore() && !targetP.isOnline()) {
                    pSender.sendMessage("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!");
                    return true;
                }

                if (pSender.getUniqueId().equals(targetP.getUniqueId())) {
                    pSender.sendMessage("§cВы не можете перевести деньги самому себе!");
                    return true;
                }

                EconomyProvider payProvider = mt.vault.api.VaultMT.getProvider();
                if (payProvider == null) {
                    pSender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    return true;
                }

                double feeRate = VaultMTP.getInstance().getConfig().getDouble("settings.transfer-fee",
                        VaultMTP.getInstance().getConfig().getDouble("transfer-fee", 0.0));

                double fee = payAmount * feeRate;
                double totalDeduction = payAmount + fee;

                if (payProvider.getBalance(pSender.getUniqueId()) < totalDeduction) {
                    if (fee > 0) {
                        pSender.sendMessage("§cУ вас недостаточно средств! Для перевода " + payAmount + "$ с комиссией " + (feeRate * 100) + "% нужно §e" + String.format("%.2f", totalDeduction) + "$");
                    } else {
                        pSender.sendMessage("§cУ вас недостаточно средств для перевода!");
                    }
                    return true;
                }

                TransactionResult withdrawResult = payProvider.withdraw(pSender.getUniqueId(), totalDeduction);

                if (withdrawResult.isSuccess()) {
                    TransactionResult depositResult = payProvider.deposit(targetP.getUniqueId(), payAmount);

                    if (depositResult.isSuccess()) {
                        if (fee > 0) {
                            pSender.sendMessage("§aВы успешно перевели §e" + payAmount + "$ §fигроку §b" + targetP.getName() + " §7(Удержана комиссия: " + String.format("%.2f", fee) + "$)");
                        } else {
                            pSender.sendMessage("§aВы успешно перевели §e" + payAmount + "$ §fигроку §b" + targetP.getName());
                        }

                        if (targetP.isOnline() && targetP.getPlayer() != null) {
                            targetP.getPlayer().sendMessage("§aИгрок §b" + pSender.getName() + " §aперевел вам §e" + payAmount + "$");
                        }
                    } else {
                        payProvider.deposit(pSender.getUniqueId(), totalDeduction);
                        pSender.sendMessage("§cОшибка перевода! Деньги возвращены на ваш счет.");
                        VaultMTP.getInstance().getLogger().warning("Сбой транзакции /emt pay: " + depositResult.errorMessage());
                    }
                } else {
                    pSender.sendMessage("§cНе удалось списать средства с вашего счета: " + withdrawResult.errorMessage());
                }

                if (payProvider instanceof SQLiteProvider) {
                    SQLiteProvider sqlProvider = (SQLiteProvider) payProvider;
                    String currency = VaultMTP.getInstance().getConfig().getString("economy.currency.symbol", "mt");

                    // Запись в базу и в файл с передачей никнеймов!
                    sqlProvider.addLog(pSender.getUniqueId(), pSender.getName(), "Перевод игроку " + targetP.getName(), -totalDeduction, currency);
                    sqlProvider.addLog(targetP.getUniqueId(), targetP.getName(), "Получено от " + pSender.getName(), payAmount, currency);
                }
                break;

            case "log":
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /emt log <игрок> [страница]");
                    return true;
                }

                OfflinePlayer logTarget = Bukkit.getOfflinePlayer(args[1]);
                if (!logTarget.hasPlayedBefore() && !logTarget.isOnline()) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cне найден в базе!");
                    return true;
                }

                int page = 1;
                if (args.length >= 3) {
                    try {
                        page = Integer.parseInt(args[2]);
                        if (page < 1) page = 1;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cНомер страницы должен быть числом!");
                        return true;
                    }
                }

                final int finalPage = page;
                EconomyProvider prov = mt.vault.api.VaultMT.getProvider();

                if (prov instanceof SQLiteProvider) {
                    SQLiteProvider sqlProvider = (SQLiteProvider) prov;

                    PlatformUtil.runAsync(VaultMTP.getInstance(), () -> {
                        List<String> logs = sqlProvider.getLogs(logTarget.getUniqueId(), finalPage);

                        PlatformUtil.runSync(VaultMTP.getInstance(), () -> {
                            sender.sendMessage("§7=== §eЛоги игрока " + logTarget.getName() + " (Стр " + finalPage + ") §7===");
                            if (logs.isEmpty()) {
                                sender.sendMessage("§cЗаписей не найдено.");
                            } else {
                                for (String log : logs) {
                                    sender.sendMessage(log);
                                }
                            }
                        });
                    });
                } else {
                    sender.sendMessage("§cЛоги доступны только при использовании встроенной базы данных (SQLite/MySQL)!");
                }
                break;

            default:
                sender.sendMessage("§cНеизвестная команда! Используйте §e/emt help");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7================ §eПомощь EMT §7================");
        sender.sendMessage("§e/emt help §7— Показать это меню");
        sender.sendMessage("§e/emt balance [игрок] §7— Узнать баланс");
        sender.sendMessage("§e/emt pay <игрок> <сумма> §7— Перевести деньги игроку");
        sender.sendMessage("§e/emt stats §7— Показать экономическую статистику сервера");
        if (sender.hasPermission("vaultmt.admin")) {
            sender.sendMessage("§e/emt give <игрок> <сумма> §7— Выдать монеты игроку");
            sender.sendMessage("§e/emt take <игрок> <сумма> §7— Забрать монеты у игрока");
            sender.sendMessage("§e/emt set <игрок> <сумма> §7— Установить точный баланс");
            sender.sendMessage("§e/emt log <игрок> §7— Посмотреть историю транзакций");
            sender.sendMessage("§e/emt version §7— Показать версию плагина");
            sender.sendMessage("§e/emt reload §7— Перезагрузить конфигурацию плагина");
        }
        sender.sendMessage("§7=========================================");
    }
}