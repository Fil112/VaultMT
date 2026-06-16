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
import java.util.concurrent.CompletableFuture;

public class MySQLProvider implements EconomyProvider {

    private Connection connection;
    private final VaultPlatform platform;

    public MySQLProvider(VaultPlatform platform) {
        this.platform = platform;
    }

    public void connect() {
        try {
            String host = platform.getConfigString("database.mysql.host", "localhost");
            String port = platform.getConfigString("database.mysql.port", "3306");
            String database = platform.getConfigString("database.mysql.database", "vaultmt");
            String username = platform.getConfigString("database.mysql.username", "root");
            String password = platform.getConfigString("database.mysql.password", "");
            boolean useSSL = platform.getConfigBoolean("database.mysql.use-ssl", false);

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=" + useSSL;
            connection = DriverManager.getConnection(url, username, password);

            initTables();
            platform.info("База данных MySQL успешно подключена!");
        } catch (SQLException e) {
            platform.severe("Ошибка при подключении к MySQL! Проверьте настройки в конфигурации.");
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
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36), " +
                    "action TEXT, " +
                    "amount DOUBLE, " +
                    "currency VARCHAR(32), " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getDefaultCurrency() {
        return platform.getConfigString("economy.default-currency", "mt");
    }

    @Override
    public String getName() {
        return "VaultMT-MySQL";
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
        double startBal = platform.getConfigDouble("economy.start-balance", 100.0);
        setBalance(uuid, startBal, getDefaultCurrency());
        return true;
    }

    @Override
    public double getBalance(UUID uuid) {
        return getBalance(uuid, getDefaultCurrency());
    }

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

    @Override
    public TransactionResult setBalance(UUID uuid, double amount, String currency) {
        // Специфичный для MySQL синтаксис обновления существующих записей
        String sql = "INSERT INTO mt_balances (uuid, currency, amount) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE amount = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, currency.toLowerCase());
            pstmt.setDouble(3, amount);
            pstmt.setDouble(4, amount);
            pstmt.executeUpdate();
            return TransactionResult.success(amount, amount, "sync_update");
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

    @Override
    public CompletableFuture<Boolean> hasAccountAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> hasAccount(uuid));
    }

    @Override
    public CompletableFuture<Boolean> createAccountAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> createAccount(uuid));
    }

    @Override
    public CompletableFuture<Double> getBalanceAsync(UUID uuid, String currency) {
        return CompletableFuture.supplyAsync(() -> getBalance(uuid, currency));
    }

    @Override
    public CompletableFuture<TransactionResult> setBalanceAsync(UUID uuid, double amount, String currency, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            TransactionResult res = setBalance(uuid, amount, currency);
            if (res.isSuccess()) {
                return TransactionResult.success(amount, amount, reason);
            }
            return res;
        });
    }

    @Override
    public CompletableFuture<TransactionResult> depositAsync(UUID uuid, double amount, String currency, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            TransactionResult res = deposit(uuid, amount, currency);
            if (res.isSuccess()) {
                return TransactionResult.success(amount, res.balance(), reason);
            }
            return res;
        });
    }

    @Override
    public CompletableFuture<TransactionResult> withdrawAsync(UUID uuid, double amount, String currency, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            TransactionResult res = withdraw(uuid, amount, currency);
            if (res.isSuccess()) {
                return TransactionResult.success(amount, res.balance(), reason);
            }
            return res;
        });
    }

    public void addLog(UUID uuid, String playerName, String action, double amount, String currency) {
        platform.runAsync(() -> {
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

            try {
                File logsFolder = new File(platform.getPluginFolder(), "logs");
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
                platform.severe("Ошибка записи лога в файл для " + playerName + ": " + e.getMessage());
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