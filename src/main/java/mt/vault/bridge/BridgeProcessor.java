package mt.vault.bridge;

import mt.vault.core.Provider;
import mt.vault.core.VaultPlatform;

import java.util.UUID;

/**
 * Внутренний обработчик запросов от шлюза.
 * Отвечает за валидацию, логирование и вызов методов экономики.
 */
public class BridgeProcessor {

    private final VaultPlatform platform;
    private final Provider providerManager; // Добавили твой Provider

    // Передаем и платформу (для логов), и провайдер (для экономики)
    public BridgeProcessor(VaultPlatform platform, Provider providerManager) {
        this.platform = platform;
        this.providerManager = providerManager;
    }

    public boolean processWithdraw(String sourcePlugin, UUID player, double amount) {
        if (!validateAmount(sourcePlugin, "Withdraw", amount)) return false;

        platform.info("[Bridge - " + sourcePlugin + "] Запрос на списание " + amount + " от " + player.toString());

        // Получаем объект TransactionResult вместо boolean
        mt.vault.api.TransactionResult result = providerManager.get().withdraw(player, amount);

        // Проверяем успешность транзакции (замени .isSuccess() на метод из твоего класса TransactionResult)
        if (result.isSuccess()) {
            platform.info("[Bridge - " + sourcePlugin + "] Успешно списано.");
            return true;
        } else {
            platform.warning("[Bridge - " + sourcePlugin + "] Отказ: Транзакция отклонена для " + player.toString());
            return false;
        }
    }

    public boolean processDeposit(String sourcePlugin, UUID player, double amount) {
        if (!validateAmount(sourcePlugin, "Deposit", amount)) return false;

        platform.info("[Bridge - " + sourcePlugin + "] Запрос на начисление " + amount + " для " + player.toString());

        // Получаем объект TransactionResult
        mt.vault.api.TransactionResult result = providerManager.get().deposit(player, amount);

        return result.isSuccess(); // Возвращаем статус успешности
    }

    public double processBalanceCheck(String sourcePlugin, UUID player) {
        // Используем твой .get()
        return providerManager.get().getBalance(player);
    }

    /**
     * Внутренняя защита от плохих плагинов, которые могут передать отрицательное число
     */
    private boolean validateAmount(String sourcePlugin, String action, double amount) {
        if (amount <= 0) {
            platform.severe("[Bridge - " + sourcePlugin + "] КРИТИЧЕСКАЯ ОШИБКА! Попытка " + action + " с некорректной суммой: " + amount);
            return false;
        }
        return true;
    }
}