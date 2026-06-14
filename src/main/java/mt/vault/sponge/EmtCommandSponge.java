package mt.vault.sponge;

import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;
import mt.vault.api.VaultMT;
import mt.vault.core.SQLiteProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import java.util.Optional;
import java.util.UUID;

public class EmtCommandSponge {

    // Вспомогательный метод для конвертации строк с '§' в формат Sponge Adventure
    private static Component text(String msg) {
        return LegacyComponentSerializer.legacySection().deserialize(msg);
    }

    // Главный метод регистрации команды (добавлен аргумент event!)
    public static void register(RegisterCommandEvent<Command.Parameterized> event, PluginContainer pluginContainer) {

        // Говорим Sponge собрать все аргументы после /emt в один список
        Parameter.Value<String> argsParam = Parameter.remainingJoinedStrings().key("args").optional().build();

        Command.Parameterized emtCommand = Command.builder()
                .addParameter(argsParam)
                .executor(context -> {
                    // Кто отправил команду (игрок или консоль)
                    org.spongepowered.api.command.CommandCause sender = context.cause();

                    // Получаем аргументы
                    Optional<String> rawArgs = context.one(argsParam);
                    String[] args = rawArgs.map(s -> s.split(" ")).orElse(new String[0]);

                    if (args.length == 0 || args[0].isEmpty()) {
                        sendHelp(sender);
                        return CommandResult.success();
                    }

                    String subCommand = args[0].toLowerCase();

                    switch (subCommand) {
                        case "reload":
                            if (!sender.hasPermission("vaultmt.admin")) {
                                sender.sendMessage(text("§cУ вас нет прав!")); // Заменишь на LangManager
                                return CommandResult.success();
                            }
                            // Здесь будет твой код перезагрузки для Sponge
                            sender.sendMessage(text("§aКонфигурация плагина перезагружена!"));
                            break;

                        case "help":
                            sendHelp(sender);
                            break;

                        case "version":
                            sender.sendMessage(text("§7====================================="));
                            sender.sendMessage(text("§fPlugin: §fVaultMT (§eEMT§f) Sponge Edition"));
                            sender.sendMessage(text("§fVersion: §f0.0.5"));
                            sender.sendMessage(text("§7====================================="));
                            break;

                        case "give":
                        case "set":
                        case "take":
                            if (!sender.hasPermission("vaultmt.admin")) {
                                sender.sendMessage(text("§cУ вас нет прав!"));
                                return CommandResult.success();
                            }

                            if (args.length < 3) {
                                sender.sendMessage(text("§cИспользование: /emt " + subCommand + " <игрок> <сумма>"));
                                return CommandResult.success();
                            }

                            double amount;
                            try {
                                amount = Double.parseDouble(args[2]);
                                if (amount < 0) {
                                    sender.sendMessage(text("§cСумма не может быть отрицательной!"));
                                    return CommandResult.success();
                                }
                            } catch (NumberFormatException e) {
                                sender.sendMessage(text("§cОшибка: §e" + args[2] + " §cне является числом!"));
                                return CommandResult.success();
                            }

                            // В Sponge поиск оффлайн-игрока работает через UserManager
                            Optional<User> targetUserOpt = Sponge.server().userManager().load(args[1]).join();
                            if (!targetUserOpt.isPresent()) {
                                sender.sendMessage(text("§cИгрок §e" + args[1] + " §cникогда не заходил на сервер!"));
                                return CommandResult.success();
                            }
                            User targetOffline = targetUserOpt.get();
                            UUID targetUUID = targetOffline.uniqueId();

                            EconomyProvider provider = VaultMT.getProvider();
                            if (provider == null) {
                                sender.sendMessage(text("§cОшибка: Экономика временно недоступна!"));
                                return CommandResult.success();
                            }

                            TransactionResult result = null;

                            if (subCommand.equals("give")) {
                                result = provider.deposit(targetUUID, amount);
                                if (result.isSuccess()) sender.sendMessage(text("§aВы выдали §e" + amount + "$ §fигроку §b" + targetOffline.name()));
                            } else if (subCommand.equals("set")) {
                                result = provider.setBalance(targetUUID, amount);
                                if (result.isSuccess()) sender.sendMessage(text("§aВы установили баланс §e" + amount + "$ §fигроку §b" + targetOffline.name()));
                            } else if (subCommand.equals("take")) {
                                result = provider.withdraw(targetUUID, amount);
                                if (result.isSuccess()) {
                                    sender.sendMessage(text("§aВы забрали §e" + amount + "$ §fу игрока §b" + targetOffline.name()));
                                } else if (result.status() == TransactionResult.Status.INSUFFICIENT_FUNDS) {
                                    sender.sendMessage(text("§cУ игрока недостаточно средств! Текущий баланс: §e" + provider.getBalance(targetUUID) + "$"));
                                    return CommandResult.success();
                                }
                            }

                            if (result != null && !result.isSuccess() && result.status() != TransactionResult.Status.INSUFFICIENT_FUNDS) {
                                sender.sendMessage(text("§cПроизошла ошибка при операции: " + result.errorMessage()));
                                return CommandResult.success();
                            }

                            // Оповещение игрока, если он онлайн
                            Optional<ServerPlayer> onlineTarget = targetOffline.player();
                            if (result != null && result.isSuccess() && onlineTarget.isPresent()) {
                                if (subCommand.equals("give")) onlineTarget.get().sendMessage(text("§aВам выдано §e" + amount + "$"));
                                else if (subCommand.equals("set")) onlineTarget.get().sendMessage(text("§6Ваш баланс установлен на §e" + amount + "$"));
                                else if (subCommand.equals("take")) onlineTarget.get().sendMessage(text("§cУ вас изъяли §e" + amount + "$"));
                            }
                            break;

                        case "bal":
                        case "balance":
                            User balTarget = null;

                            if (args.length > 1) {
                                Optional<User> userOpt = Sponge.server().userManager().load(args[1]).join();
                                if (!userOpt.isPresent()) {
                                    sender.sendMessage(text("§cИгрок не найден!"));
                                    return CommandResult.success();
                                }
                                balTarget = userOpt.get();
                            } else if (sender.subject() instanceof ServerPlayer) {
                                balTarget = ((ServerPlayer) sender.subject()).user();
                            } else {
                                sender.sendMessage(text("§cКонсоль должна указывать ник: /emt balance <игрок>"));
                                return CommandResult.success();
                            }

                            EconomyProvider balProvider = VaultMT.getProvider();
                            if (balProvider == null) {
                                sender.sendMessage(text("§cЭкономика недоступна!"));
                                return CommandResult.success();
                            }

                            double bal = balProvider.getBalance(balTarget.uniqueId());
                            sender.sendMessage(text("§eБаланс §b" + balTarget.name() + "§e: §a" + String.format("%.2f", bal) + "$"));
                            break;

                        case "pay":
                            if (!(sender.subject() instanceof ServerPlayer)) {
                                sender.sendMessage(text("§cТолько игроки могут переводить деньги!"));
                                return CommandResult.success();
                            }

                            ServerPlayer pSender = (ServerPlayer) sender.subject();

                            if (args.length < 3) {
                                pSender.sendMessage(text("§cИспользование: /emt pay <игрок> <сумма>"));
                                return CommandResult.success();
                            }

                            double payAmount;
                            try {
                                payAmount = Double.parseDouble(args[2]);
                                if (payAmount <= 0) {
                                    pSender.sendMessage(text("§cСумма должна быть больше нуля!"));
                                    return CommandResult.success();
                                }
                            } catch (NumberFormatException e) {
                                pSender.sendMessage(text("§cОшибка: неверное число!"));
                                return CommandResult.success();
                            }

                            Optional<User> targetPOpt = Sponge.server().userManager().load(args[1]).join();
                            if (!targetPOpt.isPresent()) {
                                pSender.sendMessage(text("§cИгрок §e" + args[1] + " §cне найден!"));
                                return CommandResult.success();
                            }
                            User targetP = targetPOpt.get();

                            if (pSender.uniqueId().equals(targetP.uniqueId())) {
                                pSender.sendMessage(text("§cНельзя перевести деньги самому себе!"));
                                return CommandResult.success();
                            }

                            EconomyProvider payProvider = VaultMT.getProvider();
                            if (payProvider == null) return CommandResult.success();

                            // Заглушка для комиссии (в будущем привяжешь к конфигу Sponge)
                            double feeRate = 0.0;
                            double fee = payAmount * feeRate;
                            double totalDeduction = payAmount + fee;

                            if (payProvider.getBalance(pSender.uniqueId()) < totalDeduction) {
                                pSender.sendMessage(text("§cУ вас недостаточно средств!"));
                                return CommandResult.success();
                            }

                            TransactionResult withdrawResult = payProvider.withdraw(pSender.uniqueId(), totalDeduction);

                            if (withdrawResult.isSuccess()) {
                                TransactionResult depositResult = payProvider.deposit(targetP.uniqueId(), payAmount);

                                if (depositResult.isSuccess()) {
                                    pSender.sendMessage(text("§aВы перевели §e" + payAmount + "$ §fигроку §b" + targetP.name()));
                                    targetP.player().ifPresent(p -> p.sendMessage(text("§aИгрок §b" + pSender.name() + " §aперевел вам §e" + payAmount + "$")));
                                } else {
                                    payProvider.deposit(pSender.uniqueId(), totalDeduction);
                                    pSender.sendMessage(text("§cОшибка перевода! Деньги возвращены."));
                                }
                            }
                            break;

                        default:
                            sender.sendMessage(text("§cНеизвестная команда! Используйте §e/emt help"));
                            break;
                    }

                    return CommandResult.success();
                })
                .build();

        // Регистрируем команду в Sponge API правильно, используя переданный event
        event.register(pluginContainer, emtCommand, "emt", "economy", "money");
    }

    private static void sendHelp(org.spongepowered.api.command.CommandCause sender) {
        sender.sendMessage(text("§7================ §eПомощь EMT §7================"));
        sender.sendMessage(text("§e/emt help §7— Показать это меню"));
        sender.sendMessage(text("§e/emt balance [игрок] §7— Узнать баланс"));
        sender.sendMessage(text("§e/emt pay <игрок> <сумма> §7— Перевести деньги"));
        if (sender.hasPermission("vaultmt.admin")) {
            sender.sendMessage(text("§e/emt give <игрок> <сумма> §7— Выдать монеты"));
            sender.sendMessage(text("§e/emt take <игрок> <сумма> §7— Забрать монеты"));
            sender.sendMessage(text("§e/emt set <игрок> <сумма> §7— Установить баланс"));
        }
        sender.sendMessage(text("§7========================================="));
    }
}