package ru.arixcompany.utils;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public interface IMinecraft {
    Minecraft mc = Minecraft.getInstance();
    Window window = mc.getWindow();
}
