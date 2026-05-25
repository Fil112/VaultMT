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

    /**
     * Получает текущий баланс игрока.
     *
     * @param uuid уникальный идентификатор игрока
     * @return количество средств на счету (0.0, если счета нет)
     */
    double getBalance(UUID uuid);

    /**
     * Принудительно устанавливает точное значение баланса игроку.
     *
     * @param uuid   уникальный идентификатор игрока
     * @param amount новая сумма баланса
     * @return TransactionResult с результатом выполнения операции
     */
    TransactionResult setBalance(UUID uuid, double amount);

    /**
     * Пополняет счет игрока на указанную сумму.
     *
     * @param uuid   уникальный идентификатор игрока
     * @param amount сумма для пополнения (должна быть > 0)
     * @return TransactionResult с результатом выполнения операции
     */
    TransactionResult deposit(UUID uuid, double amount);

    /**
     * Снимает средства со счета игрока.
     *
     * @param uuid   уникальный идентификатор игрока
     * @param amount сумма для снятия (должна быть > 0)
     * @return TransactionResult с результатом выполнения операции
     */
    TransactionResult withdraw(UUID uuid, double amount);

    void close();
}