package mt.vault.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class LegacyAdapter {

    private static boolean uuidSupported = false;
    private static Method getUniqueIdMethod = null;

    // Статический блок самостоятельно проверяет наличие метода getUniqueId, без помощи внешних сканеров
    static {
        try {
            getUniqueIdMethod = OfflinePlayer.class.getMethod("getUniqueId");
            uuidSupported = true;
        } catch (NoSuchMethodException e) {
            Bukkit.getLogger().warning("[VaultMT-Legacy] Метод getUniqueId не найден на этом ядре. Переход в режим совместимости (оффлайн UUID по нику).");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[VaultMT-Legacy] Ошибка при проверке поддержки UUID.");
        }
    }

    public static UUID getUUID(OfflinePlayer player) {
        if (player == null) return null;
        if (uuidSupported && getUniqueIdMethod != null) {
            try {
                return (UUID) getUniqueIdMethod.invoke(player);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "[VaultMT-Legacy] Ошибка рефлексии при получении UUID для " + player.getName(), e);
            }
        }
        return generateOfflineUUID(player.getName());
    }

    public static String getName(OfflinePlayer player) {
        if (player == null) return "Unknown";
        return player.getName();
    }

    private static UUID generateOfflineUUID(String playerName) {
        if (playerName == null) playerName = "Unknown";
        // Генерируем UUID так же, как это делает ядро сервера для оффлайн-игроков на старых версиях
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }
}