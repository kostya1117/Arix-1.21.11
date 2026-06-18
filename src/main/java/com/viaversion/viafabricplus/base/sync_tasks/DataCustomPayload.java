package com.viaversion.viafabricplus.base.sync_tasks;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPayloadRegistry;
import net.minecraft.resources.Identifier;

public record DataCustomPayload(FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DataCustomPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse(SyncTasks.PACKET_SYNC_IDENTIFIER));

    public static void init() {
        CustomPayloadRegistry.registerClientbound(DataCustomPayload.ID, CustomPacketPayload.codec((value, buf) -> {
            throw new UnsupportedOperationException("DataCustomPayload is a read-only packet");
        }, buf -> new DataCustomPayload(new FriendlyByteBuf(Unpooled.copiedBuffer(buf.readSlice(buf.readableBytes()))))));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}