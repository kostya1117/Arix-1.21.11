package net.optifine.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkLayerMap<T> {
    private T[] values = (T[])(new Object[ChunkSectionLayer.VALUES.length]);
    private Supplier<T> defaultValue;

    public ChunkLayerMap(Function<ChunkSectionLayer, T> initialValue) {
        ChunkSectionLayer[] achunksectionlayer = ChunkSectionLayer.VALUES;
        this.values = (T[])(new Object[achunksectionlayer.length]);

        for (int i = 0; i < achunksectionlayer.length; i++) {
            ChunkSectionLayer chunksectionlayer = achunksectionlayer[i];
            T t = initialValue.apply(chunksectionlayer);
            this.values[chunksectionlayer.ordinal()] = t;
        }

        for (int j = 0; j < this.values.length; j++) {
            if (this.values[j] == null) {
                throw new RuntimeException("Missing value at index: " + j);
            }
        }
    }

    public T get(ChunkSectionLayer layer) {
        return this.values[layer.ordinal()];
    }

    public Collection<T> values() {
        return Arrays.asList(this.values);
    }
}
