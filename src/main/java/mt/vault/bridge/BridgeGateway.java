package mt.vault.bridge;

import java.util.UUID;

public class BridgeGateway {

    private static BridgeGateway instance;
    private final BridgeProcessor processor;

    // Закрытый конструктор, чтобы никто не мог создать второй шлюз
    private BridgeGateway(BridgeProcessor processor) {
        this.processor = processor;
    }

    public static void init(BridgeProcessor processor) {
        if (instance == null) {
            instance = new BridgeGateway(processor);
        }
    }

    public static BridgeGateway getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BridgeGateway еще не инициализирован ядром VaultMT!");
        }
        return instance;
    }

    // =========================================================
    // ПУБЛИЧНОЕ API ДЛЯ МОСТОВ
    // =========================================================

    /**
     * Запрос на списание средств.
     * @param sourcePlugin Название моста (например, "QSVMTB")
     * @param player Уникальный ID игрока
     * @param amount Сумма списания
     * @return true если успешно, false если нет денег или счета
     */
    public boolean withdraw(String sourcePlugin, UUID player, double amount) {
        return processor.processWithdraw(sourcePlugin, player, amount);
    }

    /**
     * Запрос на начисление средств.
     */
    public boolean deposit(String sourcePlugin, UUID player, double amount) {
        return processor.processDeposit(sourcePlugin, player, amount);
    }

    /**
     * Получение баланса игрока.
     */
    public double getBalance(String sourcePlugin, UUID player) {
        return processor.processBalanceCheck(sourcePlugin, player);
    }
}