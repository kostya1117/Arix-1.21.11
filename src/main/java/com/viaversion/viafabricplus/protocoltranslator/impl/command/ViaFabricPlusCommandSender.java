package com.viaversion.viafabricplus.protocoltranslator.impl.command;

import com.viaversion.viaversion.api.command.ViaCommandSender;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ViaFabricPlusCommandSender implements ViaCommandSender {

    private final ClientSuggestionProvider source;

    public ViaFabricPlusCommandSender(final ClientSuggestionProvider source) {
        this.source = source;
    }

    @Override
    public boolean hasPermission(String s) {
        return true;
    }

    @Override
    public void sendMessage(String s) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.nullToEmpty(s.replace("/viaversion", "/viafabricplus"))
        );
    }

    @Override
    public UUID getUUID() {
        return Objects.requireNonNull(Minecraft.getInstance().player).getUUID();
    }

    @Override
    public String getName() {
        return Objects.requireNonNull(Minecraft.getInstance().player).getName().getString();
    }

}