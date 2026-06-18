package com.viaversion.viafabricplus.injection.mixin.base.access;

import com.viaversion.viafabricplus.injection.access.base.bedrock.IChunkTracker;
import com.viaversion.viaversion.libs.fastutil.longs.Long2ObjectMap;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

import java.lang.reflect.Field;
import java.util.Set;

public class MixinChunkTracker implements IChunkTracker {

    private static final Field FIELD_SUB_CHUNK_REQUESTS;
    private static final Field FIELD_PENDING_SUB_CHUNKS;
    private static final Field FIELD_CHUNKS;

    static {
        try {
            FIELD_SUB_CHUNK_REQUESTS = ChunkTracker.class.getDeclaredField("subChunkRequests");
            FIELD_SUB_CHUNK_REQUESTS.setAccessible(true);

            FIELD_PENDING_SUB_CHUNKS = ChunkTracker.class.getDeclaredField("pendingSubChunks");
            FIELD_PENDING_SUB_CHUNKS.setAccessible(true);

            FIELD_CHUNKS = ChunkTracker.class.getDeclaredField("chunks");
            FIELD_CHUNKS.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access ChunkTracker fields", e);
        }
    }

    private final ChunkTracker tracker;

    public MixinChunkTracker(ChunkTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public int viaFabricPlus$getSubChunkRequests() {
        try {
            return ((Set<?>) FIELD_SUB_CHUNK_REQUESTS.get(this.tracker)).size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int viaFabricPlus$getPendingSubChunks() {
        try {
            return ((Set<?>) FIELD_PENDING_SUB_CHUNKS.get(this.tracker)).size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int viaFabricPlus$getChunks() {
        try {
            return ((Long2ObjectMap<?>) FIELD_CHUNKS.get(this.tracker)).size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}