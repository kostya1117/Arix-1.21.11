package net.minecraft.network;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
    static void handleInboundTerminalPacket(ChannelHandlerContext p_327743_, Packet<?> p_336039_) {
        if (p_336039_.isTerminal()) {
            if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20_3)) {
                p_327743_.channel().config().setAutoRead(false);
            }
            p_327743_.pipeline().addBefore(p_327743_.name(), "inbound_config", new UnconfiguredPipelineHandler.Inbound());
            p_327743_.pipeline().remove(p_327743_.name());
        }
    }

    static void handleOutboundTerminalPacket(ChannelHandlerContext p_330082_, Packet<?> p_328185_) {
        if (p_328185_.isTerminal()) {
            p_330082_.pipeline().addAfter(p_330082_.name(), "outbound_config", new UnconfiguredPipelineHandler.Outbound());
            p_330082_.pipeline().remove(p_330082_.name());
        }
    }
}
