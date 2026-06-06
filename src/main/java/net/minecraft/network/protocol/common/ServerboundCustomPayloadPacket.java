package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPayloadRegistry;

public record ServerboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ServerCommonPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 32767;
    public static final StreamCodec<FriendlyByteBuf, ServerboundCustomPayloadPacket> STREAM_CODEC = CustomPayloadRegistry.serverboundCodec(MAX_PAYLOAD_SIZE)
            .map(ServerboundCustomPayloadPacket::new, ServerboundCustomPayloadPacket::payload);

    @Override
    public PacketType<ServerboundCustomPayloadPacket> type() {
        return CommonPacketTypes.SERVERBOUND_CUSTOM_PAYLOAD;
    }

    public void handle(ServerCommonPacketListener p_297991_) {
        p_297991_.handleCustomPayload(this);
    }
}
