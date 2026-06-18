package com.viaversion.viafabricplus.injection.mixin.base.access;

import com.viaversion.viafabricplus.injection.access.base.bedrock.IRakSessionCodec;
import io.netty.util.collection.IntObjectMap;
import org.cloudburstmc.netty.channel.raknet.packet.EncapsulatedPacket;
import org.cloudburstmc.netty.channel.raknet.packet.RakDatagramPacket;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.netty.util.FastBinaryMinHeap;

import java.lang.reflect.Field;

public class MixinRakSessionCodec implements IRakSessionCodec {

    private static final Field FIELD_OUTGOING_PACKETS;
    private static final Field FIELD_SENT_DATAGRAMS;

    static {
        try {
            FIELD_OUTGOING_PACKETS = RakSessionCodec.class.getDeclaredField("outgoingPackets");
            FIELD_OUTGOING_PACKETS.setAccessible(true);

            FIELD_SENT_DATAGRAMS = RakSessionCodec.class.getDeclaredField("sentDatagrams");
            FIELD_SENT_DATAGRAMS.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access RakSessionCodec fields", e);
        }
    }

    private final RakSessionCodec codec;

    public MixinRakSessionCodec(RakSessionCodec codec) {
        this.codec = codec;
    }

    @Override
    public int viaFabricPlus$getOutgoingPackets() {
        try {
            return ((FastBinaryMinHeap<EncapsulatedPacket>) FIELD_OUTGOING_PACKETS.get(this.codec)).size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int viaFabricPlus$SentDatagrams() {
        try {
            return ((IntObjectMap<RakDatagramPacket>) FIELD_SENT_DATAGRAMS.get(this.codec)).size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}