package net.minecraft.network.protocol.game;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.player.Abilities;

public class ServerboundPlayerAbilitiesPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerAbilitiesPacket> STREAM_CODEC = Packet.codec(
            ServerboundPlayerAbilitiesPacket::write, ServerboundPlayerAbilitiesPacket::new
    );
    private static final int FLAG_FLYING = 2;
    private final boolean isFlying;
    private Abilities viaFabricPlus$abilities;

    public ServerboundPlayerAbilitiesPacket(Abilities p_134257_) {
        this.isFlying = p_134257_.flying;
        this.viaFabricPlus$abilities = p_134257_;
    }

    private ServerboundPlayerAbilitiesPacket(FriendlyByteBuf p_179709_) {
        byte b0 = p_179709_.readByte();
        this.isFlying = (b0 & 2) != 0;
    }

    private void write(FriendlyByteBuf p_134266_) {
        byte b0 = 0;
        if (this.isFlying) {
            b0 = (byte)(b0 | 2);
        }

        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2) && viaFabricPlus$abilities != null) {
            if (viaFabricPlus$abilities.invulnerable) b0 |= 1;
            if (viaFabricPlus$abilities.mayfly) b0 |= 4;
            if (viaFabricPlus$abilities.instabuild) b0 |= 8;
        }

        p_134266_.writeByte(b0);
    }

    @Override
    public PacketType<ServerboundPlayerAbilitiesPacket> type() {
        return GamePacketTypes.SERVERBOUND_PLAYER_ABILITIES;
    }

    public void handle(ServerGamePacketListener p_134263_) {
        p_134263_.handlePlayerAbilities(this);
    }

    public boolean isFlying() {
        return this.isFlying;
    }
}
