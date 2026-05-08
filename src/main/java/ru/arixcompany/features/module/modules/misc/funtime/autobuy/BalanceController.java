package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.arixcompany.utils.IMinecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class BalanceController implements IMinecraft {

    private int balance = -1;
    private boolean waiting = false;

    public void request() {
        if (mc.player == null) return;
        waiting = true;
        mc.player.connection.sendCommand("balance");
    }

    public void handlePacket(ClientboundSystemChatPacket packet) {

        if (!waiting) return;

        String message = packet.content().getString();

        if (!message.contains("Ваш баланс")) return;

        Matcher m = Pattern.compile("\\$(\\d[\\d,]*)").matcher(message);

        if (m.find()) {
            try {
                balance = Integer.parseInt(
                        m.group(1).replaceAll("[^0-9]", "")
                );
            } catch (Exception ignored) {}
        }

        waiting = false;
    }

}