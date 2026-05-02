package net.optifine.util;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.optifine.Config;

public class IntegratedServerUtils {
    public static ServerLevel getWorldServer() {
        Minecraft minecraft = Config.getMinecraft();
        Level level = minecraft.level;
        if (level == null) {
            return null;
        }

        if (!minecraft.isLocalServer()) {
            return null;
        }

        IntegratedServer integratedserver = minecraft.getSingleplayerServer();
        if (integratedserver == null) {
            return null;
        }

        ResourceKey<Level> resourcekey = level.dimension();
        if (resourcekey == null) {
            return null;
        }

        try {
            return integratedserver.getLevel(resourcekey);
        } catch (NullPointerException nullpointerexception) {
            return null;
        }
    }

    public static Entity getEntity(UUID uuid) {
        ServerLevel serverlevel = getWorldServer();
        return serverlevel == null ? null : serverlevel.getEntity(uuid);
    }

    public static BlockEntity getTileEntity(BlockPos pos) {
        ServerLevel serverlevel = getWorldServer();
        if (serverlevel == null) {
            return null;
        }

        ChunkAccess chunkaccess = serverlevel.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        return chunkaccess == null ? null : chunkaccess.getBlockEntity(pos);
    }
}
