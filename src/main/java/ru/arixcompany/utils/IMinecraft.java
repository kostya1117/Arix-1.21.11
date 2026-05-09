package ru.arixcompany.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public interface IMinecraft {
    Minecraft mc = Minecraft.getInstance();

    static void sendPacket(Packet<?> packet) {
        if (mc.getConnection() == null) return;

        mc.getConnection().send(packet);
    }
}
