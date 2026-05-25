package mt.vault.core;

import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SQLiteProvider implements EconomyProvider {

    private Connection connection;
    private final String url;
    private final double startBalance; // Будем брать из конфига

    public SQLiteProvider(File dataFolder, double startBalance) {
        this.startBalance = startBalance;

        // 1. Убеждаемся, что папка плагина (plugins/VaultMT) существует
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // 2. Указываем путь к нашему будущему файлу базы данных
        this.url = "jdbc:sqlite:" + new File(dataFolder, "database.db").getAbsolutePath();

        // 3. Запускаем подключение и создание таблиц
        connect();
        initTable();
    }

    /**
     * Подключается к файлу базы данных
     */
    private void connect() {
        try {
            connection = DriverManager.getConnection(url);
            VaultMTP.getInstance().logInfo("Успешное подключение к SQLite!");
        } catch (SQLException e) {
            VaultMTP.getInstance().getLogger().severe("Ошибка подключения к SQLite: " + e.getMessage());
        }
    }

    /**
     * Создает таблицу, если она еще не существует
     */
    private void initTable() {
        // SQL-запрос на создание таблицы
        String sql = "CREATE TABLE IF NOT EXISTS accounts (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "balance DOUBLE NOT NULL" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            VaultMTP.getInstance().getLogger().severe("Ошибка создания таблицы: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                VaultMTP.getInstance().logInfo("Соединение с SQLite успешно закрыто.");
            } catch (SQLException e) {
                VaultMTP.getInstance().getLogger().severe("Ошибка при закрытии SQLite: " + e.getMessage());
            }
        }
    }

    @Override
    public String getName() {
        return "VaultMT-SQLite";
    }

    // ==========================================
    // НИЖЕ ИДУТ МЕТОДЫ ИЗ ТВОЕГО ИНТЕРФЕЙСА
    // ==========================================

    @Override
    public boolean hasAccount(UUID uuid) {
        String sql = "SELECT uuid FROM accounts WHERE uuid = ?";
        // Используем PreparedStatement для защиты от SQL-инъекций
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Если есть хоть одна строка, значит аккаунт существует
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public double getBalance(UUID uuid) {
        String sql = "SELECT balance FROM accounts WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Пока оставляем заглушки для остальных методов,
    // чтобы класс не выдавал ошибку компиляции

    @Override
    public boolean createAccount(UUID uuid) {
        // Напишем на следующем шаге
        return false;
    }

    @Override
    public TransactionResult setBalance(UUID uuid, double amount) {
        return TransactionResult.failure(TransactionResult.Status.FAILURE, "Not implemented");
    }

    @Override
    public TransactionResult deposit(UUID uuid, double amount) {
        return TransactionResult.failure(TransactionResult.Status.FAILURE, "Not implemented");
    }

    @Override
    public TransactionResult withdraw(UUID uuid, double amount) {
        return TransactionResult.failure(TransactionResult.Status.FAILURE, "Not implemented");
    }
}