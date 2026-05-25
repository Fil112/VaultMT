package mt.vault.core; // Замени на свой пакет

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;

public class EmtCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Если написали просто /emt без аргументов
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                // Проверка прав
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                // Перезагрузка настроек из файлов
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
                sender.sendMessage("§Plugin: §fVaultMT (§eEMT§f)");
                sender.sendMessage("§Version: §f0.0.4");
                sender.sendMessage("§7=====================================");
                break;

            case "give":
            case "set":
            case "take":
                // Проверка прав
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                // Проверка аргументов
                if (args.length < 3) {
                    String usage = VaultMTP.getInstance().getLangManager().getMessage("usage-message")
                            .replace("{command}", subCommand);
                    sender.sendMessage(usage);
                    return true;
                }

                // Парсинг суммы с защитой от ошибок
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

                // ПОИСК ИГРОКА (УНИВЕРСАЛЬНЫЙ: ОНЛАЙН И ОФФЛАЙН)
                org.bukkit.OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(args[1]);

                if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!");
                    return true;
                }

                // Получаем провайдер экономики
                mt.vault.api.EconomyProvider provider = mt.vault.api.VaultMT.getProvider();
                if (provider == null) {
                    sender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("error-provider"));
                    return true;
                }

                // ИСПОЛНЕНИЕ ЛОГИКИ В ЗАВИСИМОСТИ ОТ КОМАНДЫ
                mt.vault.api.TransactionResult result = null;

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

                // Обработка общих ошибок
                if (result != null && !result.isSuccess() && result.status() != mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                    sender.sendMessage("§cПроизошла ошибка при операции: " + result.errorMessage());
                    return true;
                }

                // Уведомление игрока (ТОЛЬКО если он сейчас онлайн)
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
                // Команда только для админов
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-permission"));
                    return true;
                }

                mt.vault.api.EconomyProvider statsProvider = mt.vault.api.VaultMT.getProvider();
                if (statsProvider == null) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("error-provider"));
                    return true;
                }

                sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("stats-loading"));

                // 2. Запускаем асинхронную задачу, чтобы не нагружать основной поток сервера
                PlatformUtil.runAsync(VaultMTP.getInstance(), () -> {
                    double totalMoney = 0;
                    double topBalance = -1;
                    String richestPlayer = "Никто";
                    int accountCount = 0;

                    // Перебираем всех игроков, которые когда-либо заходили на сервер
                    for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                        if (statsProvider.hasAccount(op.getUniqueId())) {
                            double bal = statsProvider.getBalance(op.getUniqueId());
                            totalMoney += bal;
                            accountCount++;

                            // Ищем самого богатого
                            if (bal > topBalance) {
                                topBalance = bal;
                                richestPlayer = op.getName() != null ? op.getName() : "Неизвестно (" + op.getUniqueId() + ")";
                            }
                        }
                    }

                    // Сохраняем финальные значения для вывода
                    final double finalTotal = totalMoney;
                    final String finalRichest = richestPlayer;
                    final double finalTopBal = topBalance;
                    final int finalCount = accountCount;

                    // Возвращаемся в основной поток для отправки сообщения
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

            case "pay":
                // 1. Проверяем, что команду выполняет игрок (консоль не может платить)
                if (!(sender instanceof Player)) {
                    sender.sendMessage(VaultMTP.getInstance().getLangManager().getMessage("no-console"));
                    return true;
                }

                Player pSender = (Player) sender;

                // 2. Проверка аргументов
                if (args.length < 3) {
                    pSender.sendMessage("§cИспользование: /emt pay <игрок> <сумма>");
                    return true;
                }

                // 3. Парсинг суммы (защита от дурака)
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

                // 4. Поиск получателя (поддерживает ОФФЛАЙН игроков)
                targetOffline = Bukkit.getOfflinePlayer(args[1]);

                // Защита от создания счетов для несуществующих ников
                if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
                    pSender.sendMessage("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!");
                    return true;
                }

                // Защита от перевода самому себе
                if (pSender.getUniqueId().equals(targetOffline.getUniqueId())) {
                    pSender.sendMessage("§cВы не можете перевести деньги самому себе!");
                    return true;
                }

                // Получаем активную экономику
                provider = mt.vault.api.VaultMT.getProvider();
                if (provider == null) {
                    pSender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    return true;
                }

                // 5. Проверка баланса отправителя
                if (provider.getBalance(pSender.getUniqueId()) < payAmount) {
                    pSender.sendMessage("§cУ вас недостаточно средств для перевода!");
                    return true;
                }

                // 6. ТРАНЗАКЦИЯ (Сначала снимаем)
                mt.vault.api.TransactionResult withdrawResult = provider.withdraw(pSender.getUniqueId(), payAmount);

                if (withdrawResult.isSuccess()) {
                    // Если сняли успешно — пытаемся начислить получателю
                    mt.vault.api.TransactionResult depositResult = provider.deposit(targetOffline.getUniqueId(), payAmount);

                    if (depositResult.isSuccess()) {
                        pSender.sendMessage("§aВы успешно перевели §e" + payAmount + "$ §fигроку §b" + targetOffline.getName());

                        // Если получатель прямо сейчас онлайн — отправляем ему уведомление в чат
                        if (targetOffline.isOnline() && targetOffline.getPlayer() != null) {
                            targetOffline.getPlayer().sendMessage("§aИгрок §b" + pSender.getName() + " §aперевел вам §e" + payAmount + "$");
                        }
                    } else {
                        // РОЛЛБЭК: Если начислить не удалось (ошибка базы данных или счет не найден), возвращаем деньги
                        provider.deposit(pSender.getUniqueId(), payAmount);
                        pSender.sendMessage("§cОшибка перевода! Деньги возвращены на ваш счет.");
                        VaultMTP.getInstance().getLogger().warning("Сбой транзакции /emt pay: " + depositResult.errorMessage());
                    }
                } else {
                    pSender.sendMessage("§cНе удалось списать средства с вашего счета: " + withdrawResult.errorMessage());
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
        sender.sendMessage("§e/emt pay <игрок> <сумма> §7— Перевести деньги игроку");
        sender.sendMessage("§e/emt stats §7— Показать экономическую статистику сервера");
        if (sender.hasPermission("vaultmt.admin")) {
            sender.sendMessage("§e/emt give <игрок> <сумма> §7— Выдать монеты игроку");
            sender.sendMessage("§e/emt take <игрок> <сумма> §7— Забрать монеты у игрока");
            sender.sendMessage("§e/emt set <игрок> <сумма> §7— Установить точный баланс");
            sender.sendMessage("§e/emt version §7— Показать версию плагина");
            sender.sendMessage("§e/emt reload §7— Перезагрузить конфигурацию плагина");
        }
        sender.sendMessage("§7=========================================");
        }
}