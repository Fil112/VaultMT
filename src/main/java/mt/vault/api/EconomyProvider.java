package mt.vault.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Основной интерфейс управления финансами игроков.
 */
public interface EconomyProvider {

    /**
     * Получить название текущего провайдера базы данных (например, SQLite/MySQL).
     */
    String getName();

    boolean hasAccount(UUID uuid);
    boolean createAccount(UUID uuid);

    // ==========================================
    // СИНХРОННЫЕ МЕТОДЫ (Выполняются в главном потоке)
    // ==========================================

    double getBalance(UUID uuid);
    TransactionResult setBalance(UUID uuid, double amount);
    TransactionResult deposit(UUID uuid, double amount);
    TransactionResult withdraw(UUID uuid, double amount);

    double getBalance(UUID uuid, String currency);
    TransactionResult setBalance(UUID uuid, double amount, String currency);
    TransactionResult deposit(UUID uuid, double amount, String currency);
    TransactionResult withdraw(UUID uuid, double amount, String currency);

    // ==========================================
    // АСИНХРОННЫЕ МЕТОДЫ (Не нагружают сервер)
    // Рекомендуется для использования в сторонних плагинах!
    // ==========================================

    CompletableFuture<Boolean> hasAccountAsync(UUID uuid);
    CompletableFuture<Boolean> createAccountAsync(UUID uuid);
    CompletableFuture<Double> getBalanceAsync(UUID uuid, String currency);
    CompletableFuture<TransactionResult> setBalanceAsync(UUID uuid, double amount, String currency, String reason);
    CompletableFuture<TransactionResult> depositAsync(UUID uuid, double amount, String currency, String reason);
    CompletableFuture<TransactionResult> withdrawAsync(UUID uuid, double amount, String currency, String reason);

    /**
     * Закрывает соединение с базой данных (вызывается при выключении плагина).
     */
    void close();
}