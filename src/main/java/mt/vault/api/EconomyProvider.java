package mt.vault.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyProvider {
    String getName();
    boolean hasAccount(UUID uuid);
    boolean createAccount(UUID uuid);

    double getBalance(UUID uuid);
    TransactionResult setBalance(UUID uuid, double amount);
    TransactionResult deposit(UUID uuid, double amount);
    TransactionResult withdraw(UUID uuid, double amount);

    double getBalance(UUID uuid, String currency);
    TransactionResult setBalance(UUID uuid, double amount, String currency);
    TransactionResult deposit(UUID uuid, double amount, String currency);
    TransactionResult withdraw(UUID uuid, double amount, String currency);

    // Асинхронные методы
    CompletableFuture<Boolean> hasAccountAsync(UUID uuid);
    CompletableFuture<Boolean> createAccountAsync(UUID uuid);
    CompletableFuture<Double> getBalanceAsync(UUID uuid, String currency);
    CompletableFuture<TransactionResult> setBalanceAsync(UUID uuid, double amount, String currency, String reason);
    CompletableFuture<TransactionResult> depositAsync(UUID uuid, double amount, String currency, String reason);
    CompletableFuture<TransactionResult> withdrawAsync(UUID uuid, double amount, String currency, String reason);

    void close();
}