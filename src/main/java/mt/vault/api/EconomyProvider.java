package mt.vault.api;

import java.util.UUID;

/**
 * Стандартный интерфейс провайдера экономики.
 * Любая экономическая система, интегрируемая с VaultMT, должна реализовать этот интерфейс.
 */
public interface EconomyProvider {

    /**
     * Возвращает название текущего провайдера (например, "VaultMT-Internal" или "Essentials-Adapter").
     *
     * @return имя провайдера
     */
    String getName();

    /**
     * Проверяет, существует ли банковский счет у указанного игрока.
     *
     * @param uuid уникальный идентификатор игрока
     * @return true, если счет есть, иначе false
     */
    boolean hasAccount(UUID uuid);

    /**
     * Создает новый счет для игрока с нулевым (или стандартным) балансом.
     *
     * @param uuid уникальный идентификатор игрока
     * @return true, если счет успешно создан, false если он уже существовал или произошла ошибка
     */
    boolean createAccount(UUID uuid);

    // =====================================
    // МЕТОДЫ ДЛЯ ВАЛЮТЫ ПО УМОЛЧАНИЮ
    // =====================================

    double getBalance(UUID uuid);
    TransactionResult setBalance(UUID uuid, double amount);
    TransactionResult deposit(UUID uuid, double amount);
    TransactionResult withdraw(UUID uuid, double amount);

    // =====================================
    // МУЛЬТИВАЛЮТНЫЕ МЕТОДЫ
    // =====================================

    double getBalance(UUID uuid, String currency);
    TransactionResult setBalance(UUID uuid, double amount, String currency);
    TransactionResult deposit(UUID uuid, double amount, String currency);
    TransactionResult withdraw(UUID uuid, double amount, String currency);

    void close();
}