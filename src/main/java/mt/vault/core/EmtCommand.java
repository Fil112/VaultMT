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
                // 1. Проверка прав (важно, чтобы обычные игроки не могли перезагрузить плагин)
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage("§cУ вас нет прав для выполнения этой команды!");
                    return true;
                }

                // 2. Перезагрузка конфигурации из файла config.yml
                VaultMTP.getInstance().reloadConfig();

                // TODO в будущем: если при перезагрузке нужно будет переподключаться к базе данных
                // VaultMTP.getInstance().getProviderManager().setup();

                sender.sendMessage("§a[VaultMT] Конфигурация успешно перезагружена!");
                break;

            case "help":
                sendHelp(sender);
                break;

            case "version":
                sender.sendMessage("§7=====================================");
                sender.sendMessage("§aПлагин: §fVaultMT (§eEMT§f)");
                sender.sendMessage("§aВерсия: §f0.0.3");
                sender.sendMessage("§7=====================================");
                break;

            case "give":
            case "set":
            case "take":
                // 1. Проверка прав (обязательно для экономики)
                if (!sender.hasPermission("vaultmt.admin")) {
                    sender.sendMessage("§cУ вас нет прав для выполнения этой команды!");
                    return true;
                }

                // 2. Проверка аргументов
                if (args.length < 3) {
                    sender.sendMessage("§cИспользование: /emt " + subCommand + " <игрок> <сумма>");
                    return true;
                }

                // 3. Парсинг суммы с защитой от ошибок
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount < 0) {
                        sender.sendMessage("§cСумма не может быть отрицательной!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cОшибка: §e" + args[2] + " §cне является числом!");
                    return true;
                }

                // 4. ПОИСК ИГРОКА (УНИВЕРСАЛЬНЫЙ: ОНЛАЙН И ОФФЛАЙН)
                org.bukkit.OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(args[1]);

                if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!");
                    return true;
                }

                // Получаем провайдер экономики
                mt.vault.api.EconomyProvider provider = mt.vault.api.VaultMT.getProvider();
                if (provider == null) {
                    sender.sendMessage("§cОшибка: Экономика временно недоступна!");
                    return true;
                }

                // 5. ИСПОЛНЕНИЕ ЛОГИКИ В ЗАВИСИМОСТИ ОТ КОМАНДЫ
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

                // 6. Обработка общих ошибок
                if (result != null && !result.isSuccess() && result.status() != mt.vault.api.TransactionResult.Status.INSUFFICIENT_FUNDS) {
                    sender.sendMessage("§cПроизошла ошибка при операции: " + result.errorMessage());
                    return true;
                }

                // 7. Уведомление игрока (ТОЛЬКО если он сейчас онлайн)
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

            case "pay":
                // 1. Проверяем, что команду выполняет игрок (консоль не может платить)
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cЭту команду может использовать только игрок!");
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