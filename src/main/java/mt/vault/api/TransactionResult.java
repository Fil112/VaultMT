package mt.vault.api;

public class TransactionResult {
    private final Status status;
    private final double amount;
    private final double balance;
    private final String errorMessage;
    private final String reason;
    private final long timestamp;

    public enum Status {
        SUCCESS, FAILURE, INSUFFICIENT_FUNDS, ACCOUNT_NOT_FOUND
    }

    public TransactionResult(Status status, double amount, double balance, String errorMessage, String reason, long timestamp) {
        this.status = status;
        this.amount = amount;
        this.balance = balance;
        this.errorMessage = errorMessage;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public static TransactionResult success(double amount, double balance, String reason) {
        return new TransactionResult(Status.SUCCESS, amount, balance, null, reason, System.currentTimeMillis());
    }

    public static TransactionResult failure(Status status, String errorMessage) {
        return new TransactionResult(status, 0, 0, errorMessage, "none", System.currentTimeMillis());
    }

    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }

    // Геттеры написаны в стиле record, чтобы не ломать твой остальной код
    public Status status() { return status; }
    public double amount() { return amount; }
    public double balance() { return balance; }
    public String errorMessage() { return errorMessage; }
    public String reason() { return reason; }
    public long timestamp() { return timestamp; }
}