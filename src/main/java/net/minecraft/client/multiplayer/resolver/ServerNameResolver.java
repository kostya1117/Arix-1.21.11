package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;

import java.net.InetSocketAddress;
import java.util.Optional;

import com.viaversion.viafabricplus.base.bedrock.NetherNetInetSocketAddress;
import com.viaversion.viafabricplus.injection.access.base.bedrock.IServerAddress;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;


public class ServerNameResolver {
    public static final ServerNameResolver DEFAULT = new ServerNameResolver(
        ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService()
    );
    private final ServerAddressResolver resolver;
    final ServerRedirectHandler redirectHandler;
    private final AddressCheck addressCheck;

    @VisibleForTesting
    ServerNameResolver(ServerAddressResolver p_171887_, ServerRedirectHandler p_171888_, AddressCheck p_171889_) {
        this.resolver = p_171887_;
        this.redirectHandler = p_171888_;
        this.addressCheck = p_171889_;
    }

    public Optional<ResolvedServerAddress> resolveAddress(ServerAddress p_171891_) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
            return this.resolver.resolve(p_171891_);
        }

        if (p_171891_ instanceof IServerAddress mixinServerAddress && mixinServerAddress.viaFabricPlus$getNetherNetAddress() != null) {
            final NetherNetAddress netherNetAddress = mixinServerAddress.viaFabricPlus$getNetherNetAddress();
            return Optional.of(new ResolvedServerAddress() {
                @Override
                public String getHostName() {
                    return netherNetAddress.getNetworkId();
                }

                @Override
                public String getHostIp() {
                    return netherNetAddress.getNetworkId();
                }

                @Override
                public int getPort() {
                    return 0;
                }

                @Override
                public InetSocketAddress asInetSocketAddress() {
                    return new NetherNetInetSocketAddress(netherNetAddress);
                }
            });
        }

        if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return this.resolver.resolve(p_171891_);
        }

        Optional<ResolvedServerAddress> optional = this.resolver.resolve(p_171891_);
        if ((!optional.isPresent() || this.addressCheck.isAllowed(optional.get())) && this.addressCheck.isAllowed(p_171891_)) {
            Optional<ServerAddress> optional1 = this.redirectHandler.lookupRedirect(p_171891_);
            if (optional1.isPresent()) {
                optional = this.resolver.resolve(optional1.get()).filter(this.addressCheck::isAllowed);
            }

            return optional;
        } else {
            return Optional.empty();
        }
    }
}
