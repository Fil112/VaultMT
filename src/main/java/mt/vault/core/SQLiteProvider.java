package mt.vault.core;

import mt.vault.api.EconomyProvider;
import mt.vault.api.TransactionResult;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SQLiteProvider implements EconomyProvider {

    private Connection connection;
    private final VaultMTP plugin;

    public SQLiteProvider(VaultMTP plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            initTables();
            plugin.getLogger().info("База данных SQLite успешно подключена!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при подключении к SQLite!");
            e.printStackTrace();
        }
    }

    private void initTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS mt_balances (" +
                    "uuid VARCHAR(36), " +
                    "currency VARCHAR(32), " +
                    "amount DOUBLE, " +
                    "PRIMARY KEY(uuid, currency))");

            stmt.execute("CREATE TABLE IF NOT EXISTS mt_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "action TEXT, " +
                    "amount DOUBLE, " +
                    "currency VARCHAR(32), " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getDefaultCurrency() {
        return plugin.getConfig().getString("economy.default-currency", "mt");
    }

    // =====================================
    // БАЗОВЫЕ МЕТОДЫ ИНТЕРФЕЙСА
    // =====================================

    @Override
    public String getName() {
        return "VaultMT-SQLite";
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        String sql = "SELECT 1 FROM mt_balances WHERE uuid = ? AND currency = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, getDefaultCurrency().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createAccount(UUID uuid) {
        if (hasAccount(uuid)) return false;

        // Выдаем стартовый баланс из конфига
        double startBal = plugin.getConfig().getDouble("economy.start-balance", 100.0);
        setBalance(uuid, startBal, getDefaultCurrency());
        return true;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =====================================
    // ДЕФОЛТНАЯ ВАЛЮТА (ПЕРЕАДРЕСАЦИЯ)
    // =====================================

    @Override
    public double getBalance(UUID uuid) {
        return getBalance(uuid, getDefaultCurrency());
    }

    @Override
    public TransactionResult setBalance(UUID uuid, double amount) {
        return setBalance(uuid, amount, getDefaultCurrency());
    }

    @Override
    public TransactionResult deposit(UUID uuid, double amount) {
        return deposit(uuid, amount, getDefaultCurrency());
    }

    @Override
    public TransactionResult withdraw(UUID uuid, double amount) {
        return withdraw(uuid, amount, getDefaultCurrency());
    }

    // =====================================
    // МУЛЬТИВАЛЮТНАЯ РЕАЛИЗАЦИЯ
    // =====================================

    @Override
    public double getBalance(UUID uuid, String currency) {
        String sql = "SELECT amount FROM mt_balances WHERE uuid = ? AND currency = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, currency.toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public TransactionResult setBalance(UUID uuid, double amount, String currency) {
        String sql = "INSERT OR REPLACE INTO mt_balances (uuid, currency, amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, currency.toLowerCase());
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
            return TransactionResult.success(amount, amount);
        } catch (SQLException e) {
            e.printStackTrace();
            return TransactionResult.failure(TransactionResult.Status.FAILURE, e.getMessage());
        }
    }

    @Override
    public TransactionResult deposit(UUID uuid, double amount, String currency) {
        double current = getBalance(uuid, currency);
        return setBalance(uuid, current + amount, currency);
    }

    @Override
    public TransactionResult withdraw(UUID uuid, double amount, String currency) {
        double current = getBalance(uuid, currency);
        if (current < amount) {
            return TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_FUNDS, "Недостаточно средств");
        }
        return setBalance(uuid, current - amount, currency);
    }

    // =====================================
    // МЕТОДЫ ЛОГОВ (БД + Файлы)
    // =====================================

    public void addLog(UUID uuid, String playerName, String action, double amount, String currency) {
        PlatformUtil.runAsync(plugin, () -> {
            // 1. Запись в базу данных SQLite
            String sql = "INSERT INTO mt_logs (uuid, action, amount, currency) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, action);
                pstmt.setDouble(3, amount);
                pstmt.setString(4, currency.toLowerCase());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 2. Сохранение в отдельный текстовый файл
            try {
                File logsFolder = new File(plugin.getDataFolder(), "logs");
                if (!logsFolder.exists()) {
                    logsFolder.mkdirs();
                }

                File userFile = new File(logsFolder, playerName + "_" + uuid.toString() + ".txt");
                boolean isNewFile = !userFile.exists();

                try (FileWriter fw = new FileWriter(userFile, true);
                     PrintWriter pw = new PrintWriter(fw)) {

                    if (isNewFile) {
                        pw.println("=========================================");
                        pw.println("Финансовая история пользователя");
                        pw.println("Игрок: " + playerName);
                        pw.println("UUID: " + uuid.toString());
                        pw.println("Создано: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                        pw.println("=========================================");
                    }

                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    pw.println("[" + time + "] " + action + " | Сумма: " + amount + " " + currency);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Ошибка записи лога в файл для " + playerName + ": " + e.getMessage());
            }
        });
    }

    public List<String> getLogs(UUID uuid, int page) {
        List<String> logs = new ArrayList<>();
        int limit = 7;
        int offset = (page - 1) * limit;
        String sql = "SELECT * FROM mt_logs WHERE uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String time = rs.getString("timestamp");
                String action = rs.getString("action");
                double amount = rs.getDouble("amount");
                String currency = rs.getString("currency");

                logs.add("§8[" + time + "] §e" + action + " §a" + amount + " §7" + currency);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}