package mt.vault.sponge;

import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;
import mt.vault.api.VaultMT;
import mt.vault.core.VaultPlatform;
import mt.vault.core.SQLiteProvider;
import mt.vault.core.MySQLProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

public class EmtCommandSponge {

    private static Component text(String msg) {
        return LegacyComponentSerializer.legacySection().deserialize(msg);
    }

    public static void register(RegisterCommandEvent<Command.Parameterized> event, PluginContainer pluginContainer) {

        Parameter.Value<String> argsParam = Parameter.remainingJoinedStrings().key("args").optional().build();

        Command.Parameterized emtCommand = Command.builder()
                .addParameter(argsParam)
                .executor(context -> {
                    LanguageManager lang = VaultMTSponge.getLang();
                    org.spongepowered.api.command.CommandCause sender = context.cause();

                    Optional<String> rawArgs = context.one(argsParam);
                    String[] args = rawArgs.map(s -> s.split(" ")).orElse(new String[0]);

                    if (args.length == 0 || args[0].isEmpty()) {
                        sendHelp(sender);
                        return CommandResult.success();
                    }

                    String subCommand = args[0].toLowerCase();
                    VaultPlatform platform = VaultMTSponge.getInstance().getPlatform();

                    switch (subCommand) {
                        case "reload":
                            if (!sender.hasPermission("vaultmt.admin")) {
                                sender.sendMessage(text(lang.getMessage("no-permission")));
                                return CommandResult.success();
                            }

                            lang.loadMessages();
                            // В идеале тут нужно добавить platform.loadConfig(),
                            // если ты реализуешь перезагрузку файла настроек "на лету"
                            sender.sendMessage(text(lang.getMessage("reload-success")));
                            break;

                        case "help":
                            sendHelp(sender);
                            break;

                        case "version":
                            sender.sendMessage(text("§7====================================="));
                            sender.sendMessage(text("§fPlugin: §fVaultMT (§eEMT§f) Sponge Edition"));
                            sender.sendMessage(text("§fVersion: §f1.0.0-Release"));
                            sender.sendMessage(text("§7====================================="));
                            break;

                        case "give":
                        case "set":
                        case "take":
                            if (!sender.hasPermission("vaultmt.admin")) {
                                sender.sendMessage(text(lang.getMessage("no-permission")));
                                return CommandResult.success();
                            }

                            if (args.length < 3) {
                                String msg = lang.getMessage("usage-message").replace("{command}", subCommand);
                                sender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            double amount;
                            try {
                                amount = Double.parseDouble(args[2]);
                                if (amount < 0) {
                                    sender.sendMessage(text(lang.getMessage("no-amount")));
                                    return CommandResult.success();
                                }
                            } catch (NumberFormatException e) {
                                String msg = lang.getMessage("not-a-number").replace("{value}", args[2]);
                                sender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            Optional<User> targetUserOpt = Sponge.server().userManager().load(args[1]).join();
                            if (!targetUserOpt.isPresent()) {
                                String msg = lang.getMessage("player-never-played").replace("{player}", args[1]);
                                sender.sendMessage(text(msg));
                                return CommandResult.success();
                            }
                            User targetOffline = targetUserOpt.get();
                            UUID targetUUID = targetOffline.uniqueId();

                            EconomyProvider provider = VaultMT.getProvider();
                            if (provider == null) {
                                sender.sendMessage(text(lang.getMessage("error-provider")));
                                return CommandResult.success();
                            }

                            TransactionResult result = null;

                            if (subCommand.equals("give")) {
                                result = provider.deposit(targetUUID, amount);
                                if (result.isSuccess()) {
                                    String msg = lang.getMessage("give-sender")
                                            .replace("{amount}", String.valueOf(amount))
                                            .replace("{player}", targetOffline.name());
                                    sender.sendMessage(text(msg));
                                }
                            } else if (subCommand.equals("set")) {
                                result = provider.setBalance(targetUUID, amount);
                                if (result.isSuccess()) {
                                    String msg = lang.getMessage("set-sender")
                                            .replace("{amount}", String.valueOf(amount))
                                            .replace("{player}", targetOffline.name());
                                    sender.sendMessage(text(msg));
                                }
                            } else if (subCommand.equals("take")) {
                                result = provider.withdraw(targetUUID, amount);
                                if (result.isSuccess()) {
                                    String msg = lang.getMessage("take-sender")
                                            .replace("{amount}", String.valueOf(amount))
                                            .replace("{player}", targetOffline.name());
                                    sender.sendMessage(text(msg));
                                } else if (result.status() == TransactionResult.Status.INSUFFICIENT_FUNDS) {
                                    String msg = lang.getMessage("take-insufficient")
                                            .replace("{balance}", String.format("%.2f", provider.getBalance(targetUUID)));
                                    sender.sendMessage(text(msg));
                                    return CommandResult.success();
                                }
                            }

                            if (result != null && !result.isSuccess() && result.status() != TransactionResult.Status.INSUFFICIENT_FUNDS) {
                                String msg = lang.getMessage("transaction-error").replace("{error}", result.errorMessage());
                                sender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            Optional<ServerPlayer> onlineTarget = targetOffline.player();
                            if (result != null && result.isSuccess() && onlineTarget.isPresent()) {
                                String targetMsg = "";
                                if (subCommand.equals("give")) {
                                    targetMsg = lang.getMessage("give-target").replace("{amount}", String.valueOf(amount));
                                    if (targetMsg.contains("Missing message")) targetMsg = "§aВам выдано §e" + amount + "$";
                                } else if (subCommand.equals("set")) {
                                    targetMsg = lang.getMessage("set-target").replace("{amount}", String.valueOf(amount));
                                    if (targetMsg.contains("Missing message")) targetMsg = "§6Ваш баланс установлен на §e" + amount + "$";
                                } else if (subCommand.equals("take")) {
                                    targetMsg = lang.getMessage("take-target").replace("{amount}", String.valueOf(amount));
                                    if (targetMsg.contains("Missing message")) targetMsg = "§cУ вас изъяли §e" + amount + "$";
                                }
                                onlineTarget.get().sendMessage(text(targetMsg));
                            }
                            break;

                        case "bal":
                        case "balance":
                            User balTarget = null;

                            if (args.length > 1) {
                                Optional<User> userOpt = Sponge.server().userManager().load(args[1]).join();
                                if (!userOpt.isPresent()) {
                                    String msg = lang.getMessage("player-never-played").replace("{player}", args[1]);
                                    sender.sendMessage(text(msg));
                                    return CommandResult.success();
                                }
                                balTarget = userOpt.get();
                            } else if (sender.subject() instanceof ServerPlayer) {
                                balTarget = ((ServerPlayer) sender.subject()).user();
                            } else {
                                sender.sendMessage(text(lang.getMessage("console-needs-player")));
                                return CommandResult.success();
                            }

                            EconomyProvider balProvider = VaultMT.getProvider();
                            if (balProvider == null) {
                                sender.sendMessage(text(lang.getMessage("error-provider")));
                                return CommandResult.success();
                            }

                            double bal = balProvider.getBalance(balTarget.uniqueId());

                            String balMsg = lang.getMessage("balance-display");
                            if (balMsg.contains("Missing message")) {
                                balMsg = "§eБаланс §b" + balTarget.name() + "§e: §a" + String.format("%.2f", bal) + "$";
                            } else {
                                balMsg = balMsg.replace("{player}", balTarget.name()).replace("{balance}", String.format("%.2f", bal));
                            }
                            sender.sendMessage(text(balMsg));
                            break;

                        case "pay":
                            if (!(sender.subject() instanceof ServerPlayer)) {
                                sender.sendMessage(text(lang.getMessage("no-console")));
                                return CommandResult.success();
                            }

                            ServerPlayer pSender = (ServerPlayer) sender.subject();

                            if (args.length < 3) {
                                String msg = lang.getMessage("usage-message").replace("{command}", "pay");
                                pSender.sendMessage(text(msg));
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
                                String msg = lang.getMessage("not-a-number").replace("{value}", args[2]);
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            // 1. Проверка лимитов перевода из платформы
                            double minPay = platform.getConfigDouble("settings.limits.min-pay", 1.0);
                            double maxPay = platform.getConfigDouble("settings.limits.max-pay", 100000.0);

                            if (payAmount < minPay) {
                                String msg = lang.getMessage("pay-min-limit").replace("{min}", String.valueOf(minPay));
                                if (msg.contains("Missing message")) msg = "§cМинимальная сумма перевода: §e" + minPay + "$";
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            if (payAmount > maxPay) {
                                String msg = lang.getMessage("pay-max-limit").replace("{max}", String.valueOf(maxPay));
                                if (msg.contains("Missing message")) msg = "§cМаксимальная сумма перевода: §e" + maxPay + "$";
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            Optional<User> targetPOpt = Sponge.server().userManager().load(args[1]).join();
                            if (!targetPOpt.isPresent()) {
                                String msg = lang.getMessage("player-never-played").replace("{player}", args[1]);
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }
                            User targetP = targetPOpt.get();

                            if (pSender.uniqueId().equals(targetP.uniqueId())) {
                                String msg = lang.getMessage("pay-self");
                                if (msg.contains("Missing message")) msg = "§cНельзя перевести деньги самому себе!";
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            // 2. Проверка черного списка
                            List<String> blacklist = platform.getConfigStringList("settings.blacklist");
                            boolean senderBlocked = false;
                            boolean targetBlocked = false;

                            for (String blockedName : blacklist) {
                                if (blockedName.equalsIgnoreCase(pSender.name())) senderBlocked = true;
                                if (blockedName.equalsIgnoreCase(targetP.name())) targetBlocked = true;
                            }

                            if (senderBlocked) {
                                String msg = lang.getMessage("pay-blacklisted-sender");
                                if (msg.contains("Missing message")) msg = "§cВаши финансовые операции заблокированы администрацией!";
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            if (targetBlocked) {
                                String msg = lang.getMessage("pay-blacklisted-target").replace("{player}", targetP.name());
                                if (msg.contains("Missing message")) msg = "§cИгрок §b" + targetP.name() + " §cнаходитcя в черном списке для переводов!";
                                pSender.sendMessage(text(msg));
                                return CommandResult.success();
                            }

                            EconomyProvider payProvider = VaultMT.getProvider();
                            if (payProvider == null) {
                                pSender.sendMessage(text(lang.getMessage("error-provider")));
                                return CommandResult.success();
                            }

                            // 3. Динамический налог
                            double fee = calculateDynamicFee(platform, payAmount);
                            double totalDeduction = payAmount + fee;
                            double effectiveFeePercent = payAmount > 0 ? (fee / payAmount) * 100.0 : 0.0;

                            if (payProvider.getBalance(pSender.uniqueId()) < totalDeduction) {
                                if (fee > 0) {
                                    String msg = lang.getMessage("pay-insufficient-fee")
                                            .replace("{amount}", String.valueOf(payAmount))
                                            .replace("{fee}", String.format("%.1f", effectiveFeePercent))
                                            .replace("{total}", String.format("%.2f", totalDeduction));
                                    if (msg.contains("Missing message")) msg = "§cУ вас недостаточно средств! Нужно: §e" + String.format("%.2f", totalDeduction) + "$";
                                    pSender.sendMessage(text(msg));
                                } else {
                                    String msg = lang.getMessage("pay-insufficient");
                                    if (msg.contains("Missing message")) msg = "§cУ вас недостаточно средств!";
                                    pSender.sendMessage(text(msg));
                                }
                                return CommandResult.success();
                            }

                            TransactionResult withdrawResult = payProvider.withdraw(pSender.uniqueId(), totalDeduction);

                            if (withdrawResult.isSuccess()) {
                                TransactionResult depositResult = payProvider.deposit(targetP.uniqueId(), payAmount);

                                if (depositResult.isSuccess()) {
                                    if (fee > 0) {
                                        String msgSender = lang.getMessage("pay-success-fee")
                                                .replace("{amount}", String.valueOf(payAmount))
                                                .replace("{player}", targetP.name())
                                                .replace("{fee}", String.format("%.2f", fee));
                                        if (msgSender.contains("Missing message")) {
                                            msgSender = "§aВы успешно перевели §e" + payAmount + "$ §fигроку §b" + targetP.name() + " §7(Удержана комиссия: " + String.format("%.2f", fee) + "$)";
                                        }
                                        pSender.sendMessage(text(msgSender));
                                    } else {
                                        String msgSender = lang.getMessage("pay-success-sender")
                                                .replace("{amount}", String.valueOf(payAmount))
                                                .replace("{player}", targetP.name());
                                        if (msgSender.contains("Missing message")) {
                                            msgSender = "§aВы перевели §e" + payAmount + "$ §fигроку §b" + targetP.name();
                                        }
                                        pSender.sendMessage(text(msgSender));
                                    }

                                    targetP.player().ifPresent(p -> {
                                        String msgTarget = lang.getMessage("pay-success-target")
                                                .replace("{amount}", String.valueOf(payAmount))
                                                .replace("{player}", pSender.name());
                                        if (msgTarget.contains("Missing message")) {
                                            msgTarget = "§aИгрок §b" + pSender.name() + " §aперевел вам §e" + payAmount + "$";
                                        }
                                        p.sendMessage(text(msgTarget));
                                    });
                                } else {
                                    payProvider.deposit(pSender.uniqueId(), totalDeduction);
                                    String msgReturn = lang.getMessage("pay-error-return");
                                    if (msgReturn.contains("Missing message")) msgReturn = "§cОшибка перевода! Деньги возвращены.";
                                    pSender.sendMessage(text(msgReturn));
                                    VaultMTSponge.getInstance().getLogger().warn("Сбой транзакции /emt pay: " + depositResult.errorMessage());
                                }
                            } else {
                                pSender.sendMessage(text(lang.getMessage("pay-fail").replace("{error}", withdrawResult.errorMessage())));
                            }

                            // 4. Логирование транзакций (совместимость с Bukkit-версией)
                            String currency = platform.getConfigString("economy.currency.symbol", "mt");
                            if (payProvider instanceof SQLiteProvider) {
                                SQLiteProvider sqlProvider = (SQLiteProvider) payProvider;
                                sqlProvider.addLog(pSender.uniqueId(), pSender.name(), "Перевод игроку " + targetP.name(), -totalDeduction, currency);
                                sqlProvider.addLog(targetP.uniqueId(), targetP.name(), "Получено от " + pSender.name(), payAmount, currency);
                            } else if (payProvider instanceof MySQLProvider) {
                                MySQLProvider mysqlProvider = (MySQLProvider) payProvider;
                                mysqlProvider.addLog(pSender.uniqueId(), pSender.name(), "Перевод игроку " + targetP.name(), -totalDeduction, currency);
                                mysqlProvider.addLog(targetP.uniqueId(), targetP.name(), "Получено от " + pSender.name(), payAmount, currency);
                            }

                            break;

                        default:
                            sender.sendMessage(text(lang.getMessage("unknown-command")));
                            break;
                    }

                    return CommandResult.success();
                })
                .build();

        event.register(pluginContainer, emtCommand, "emt", "economy", "money");
    }

    private static double calculateDynamicFee(VaultPlatform platform, double amount) {
        boolean dynamicEnabled = platform.getConfigBoolean("settings.dynamic-tax.enabled", false);

        if (dynamicEnabled) {
            List<String> brackets = platform.getConfigStringList("settings.dynamic-tax.brackets");
            double rate = platform.getConfigDouble("settings.dynamic-tax.default-rate", 0.0);

            for (String bracket : brackets) {
                try {
                    String[] split = bracket.split(":");
                    double threshold = Double.parseDouble(split[0]);
                    double bracketRate = Double.parseDouble(split[1]);

                    if (amount >= threshold) {
                        rate = bracketRate;
                    }
                } catch (Exception ignored) {}
            }
            return amount * rate;
        }

        return amount * platform.getConfigDouble("settings.transfer-fee", 0.0);
    }

    private static void sendHelp(org.spongepowered.api.command.CommandCause sender) {
        LanguageManager lang = VaultMTSponge.getLang();
        sender.sendMessage(text(lang.getMessage("help-header")));
        sender.sendMessage(text(lang.getMessage("help-balance")));
        sender.sendMessage(text(lang.getMessage("help-pay")));
        if (sender.hasPermission("vaultmt.admin")) {
            sender.sendMessage(text(lang.getMessage("help-give")));
            sender.sendMessage(text(lang.getMessage("help-take")));
            sender.sendMessage(text(lang.getMessage("help-set")));
        }
        sender.sendMessage(text("§7========================================="));
    }
}