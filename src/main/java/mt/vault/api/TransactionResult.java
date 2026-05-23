package mt.vault.api;

/**
 * Представляет неизменяемый результат экономической транзакции.
 */
public record TransactionResult(
        Status status,
        double amount,
        double balance,
        String errorMessage
) {

    /**
     * Возможные статусы выполнения транзакции.
     */
    public enum Status {
        SUCCESS,             // Операция прошла успешно
        FAILURE,             // Общая или неизвестная ошибка
        INSUFFICIENT_FUNDS,  // У игрока недостаточно средств для снятия
        ACCOUNT_NOT_FOUND    // Банковский счет игрока не существует
    }

    /**
     * Быстрое создание успешного результата транзакции.
     *
     * @param amount  сумма транзакции
     * @param balance новый баланс после транзакции
     */
    public static TransactionResult success(double amount, double balance) {
        return new TransactionResult(Status.SUCCESS, amount, balance, null);
    }

    /**
     * Быстрое создание результата с ошибкой.
     *
     * @param status       тип ошибки (не SUCCESS)
     * @param errorMessage текстовое описание проблемы (опционально)
     */
    public static TransactionResult failure(Status status, String errorMessage) {
        return new TransactionResult(status, 0, 0, errorMessage);
    }

    /**
     * Проверяет, была ли транзакция успешной.
     */
    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }
}