package mt.vault.core; // Замени на свой пакет

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            case "help":
                sendHelp(sender);
                break;

            case "version":
                sender.sendMessage("§7=====================================");
                sender.sendMessage("§aПлагин: §fVaultMT (§eEMT§f)");
                sender.sendMessage("§aВерсия: §f1.0.0");
                sender.sendMessage("§7=====================================");
                break;

            case "give":
            case "set":
            case "take":
                // Общая проверка аргументов для команд с балансом
                if (args.length < 3) {
                    sender.sendMessage("§cИспользование: /emt " + subCommand + " <игрок> <сумма>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cне найден или оффлайн!");
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount <= 0) {
                        sender.sendMessage("§cСумма должна быть больше нуля!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cОшибка: §e" + args[2] + " §cне является числом!");
                    return true;
                }

                // Индивидуальная логика каждой подкоманды
                if (subCommand.equals("give")) {
                    sender.sendMessage("§aВы успешно выдали §e" + amount + "$ §fигроку §b" + target.getName());
                    target.sendMessage("§aВам начислено §e" + amount + "$");
                    // TODO: Экономика.добавитьБаланс(target, amount);

                } else if (subCommand.equals("set")) {
                    sender.sendMessage("§aВы установили баланс §e" + amount + "$ §fигроку §b" + target.getName());
                    target.sendMessage("§6Ваш баланс изменен на §e" + amount + "$");
                    // TODO: Экономика.установитьБаланс(target, amount);

                } else if (subCommand.equals("take")) {
                    sender.sendMessage("§aВы успешно забрали §e" + amount + "$ §fу игрока §b" + target.getName());
                    target.sendMessage("§cУ вас было изъято §e" + amount + "$");
                    // TODO: Экономика.отнятьБаланс(target, amount);
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
        sender.sendMessage("§e/emt version §7— Показать версию плагина");
        sender.sendMessage("§e/emt give <игрок> <сумма> §7— Выдать монеты игроку");
        sender.sendMessage("§e/emt take <игрок> <сумма> §7— Забрать монеты у игрока");
        sender.sendMessage("§e/emt set <игрок> <сумма> §7— Установить точный баланс");
        sender.sendMessage("§7=========================================");
    }
}