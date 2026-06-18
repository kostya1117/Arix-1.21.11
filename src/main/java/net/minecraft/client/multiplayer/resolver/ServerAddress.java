package net.minecraft.client.multiplayer.resolver;

import com.google.common.net.HostAndPort;
import com.mojang.logging.LogUtils;
import java.net.IDN;

import com.viaversion.viafabricplus.injection.access.base.bedrock.IServerAddress;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;


public final class ServerAddress implements IServerAddress {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final HostAndPort hostAndPort;
    private static final ServerAddress INVALID = new ServerAddress(HostAndPort.fromParts("server.invalid", 25565));
    private NetherNetAddress viaFabricPlus$netherNetAddress;

    public ServerAddress(String p_171861_, int p_171862_) {
        this(HostAndPort.fromParts(p_171861_, p_171862_));
    }

    private ServerAddress(HostAndPort p_171859_) {
        this.hostAndPort = p_171859_;
    }

    public String getHost() {
        try {
            return IDN.toASCII(this.hostAndPort.getHost());
        } catch (IllegalArgumentException illegalargumentexception) {
            return "";
        }
    }

    @Override
    public NetherNetAddress viaFabricPlus$getNetherNetAddress() {
        return this.viaFabricPlus$netherNetAddress;
    }

    @Override
    public void viaFabricPlus$setNetherNetAddress(final NetherNetAddress address) {
        this.viaFabricPlus$netherNetAddress = address;
    }

    public int getPort() {
        return this.hostAndPort.getPort();
    }

    public static ServerAddress parseString(@Nullable String p_171865_) {
        if (p_171865_ == null) {
            return INVALID;
        }

        try {
            HostAndPort hostandport = HostAndPort.fromString(p_171865_).withDefaultPort(25565);
            ServerAddress result = hostandport.getHost().isEmpty() ? INVALID : new ServerAddress(hostandport);

            if (!result.equals(INVALID) && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
                result = ServerNameResolver.DEFAULT.redirectHandler.lookupRedirect(result).orElse(result);
            }

            return result;
        } catch (IllegalArgumentException illegalargumentexception) {
            LOGGER.info("Failed to parse URL {}", p_171865_, illegalargumentexception);
            return INVALID;
        }
    }

    public static boolean isValidAddress(String p_171868_) {
        try {
            HostAndPort hostandport = HostAndPort.fromString(p_171868_);
            String s = hostandport.getHost();
            if (!s.isEmpty()) {
                IDN.toASCII(s);
                return true;
            }
        } catch (IllegalArgumentException illegalargumentexception) {
        }

        return false;
    }

    static int parsePort(String p_171870_) {
        try {
            return Integer.parseInt(p_171870_.trim());
        } catch (Exception exception) {
            return 25565;
        }
    }

    @Override
    public String toString() {
        return this.hostAndPort.toString();
    }

    @Override
    public boolean equals(Object p_171872_) {
        if (this == p_171872_) {
            return true;
        } else {
            return p_171872_ instanceof ServerAddress ? this.hostAndPort.equals(((ServerAddress)p_171872_).hostAndPort) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.hostAndPort.hashCode();
    }
}
