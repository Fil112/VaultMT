package mt.vault.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class BungeeMessageListener implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("vaultmt:main")) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String subChannel = in.readUTF();

            if (subChannel.equals("PAY_NOTIFY")) {
                String targetUUID = in.readUTF();
                double amount = in.readDouble();
                String senderName = in.readUTF();

                ProxiedPlayer targetPlayer = ProxyServer.getInstance().getPlayer(UUID.fromString(targetUUID));

                if (targetPlayer != null) {
                    targetPlayer.sendMessage(new TextComponent("§aВам поступил перевод: §e" + amount + " $ §aот игрока §b" + senderName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}