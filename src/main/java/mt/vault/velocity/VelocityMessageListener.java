package mt.vault.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;
import java.util.UUID;

public class VelocityMessageListener {

    private final ProxyServer server;

    public VelocityMessageListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Проверяем, что сообщение пришло по нашему каналу
        if (!event.getIdentifier().equals(VaultMTVelocity.CHANNEL)) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String subChannel = in.readUTF();

            // Обработка уведомления о переводе средств (/pay)
            if (subChannel.equals("PAY_NOTIFY")) {
                String targetUUID = in.readUTF();
                double amount = in.readDouble();
                String senderName = in.readUTF();

                // Ищем игрока глобально на всем прокси
                Optional<Player> targetPlayer = server.getPlayer(UUID.fromString(targetUUID));

                if (targetPlayer.isPresent()) {
                    Component message = Component.text("Вам поступил перевод: ", NamedTextColor.GREEN)
                            .append(Component.text(amount + " $ ", NamedTextColor.YELLOW))
                            .append(Component.text("от игрока ", NamedTextColor.GREEN))
                            .append(Component.text(senderName, NamedTextColor.AQUA));

                    targetPlayer.get().sendMessage(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}