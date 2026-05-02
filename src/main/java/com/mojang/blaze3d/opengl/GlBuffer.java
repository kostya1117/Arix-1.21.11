package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.optifine.util.GpuMemory;
import org.jspecify.annotations.Nullable;

public class GlBuffer extends GpuBuffer {
    protected static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
    protected boolean closed;
    protected final @Nullable Supplier<String> label;
    private final DirectStateAccess dsa;
    protected final int handle;
    protected @Nullable ByteBuffer persistentBuffer;

    protected GlBuffer(
        @Nullable Supplier<String> p_394612_,
        DirectStateAccess p_407552_,
        @GpuBuffer.Usage int p_395014_,
        long p_451380_,
        int p_395070_,
        @Nullable ByteBuffer p_408413_
    ) {
        super(p_395014_, p_451380_);
        this.label = p_394612_;
        this.dsa = p_407552_;
        this.handle = p_395070_;
        this.persistentBuffer = p_408413_;
        int i = (int)Math.min(p_451380_, 2147483647L);
        MEMORY_POOl.malloc(p_395070_, i);
        GpuMemory.bufferAllocated(p_451380_);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.persistentBuffer != null) {
                this.dsa.unmapBuffer(this.handle, this.usage());
                this.persistentBuffer = null;
            }

            GlStateManager._glDeleteBuffers(this.handle);
            MEMORY_POOl.free(this.handle);
            GpuMemory.bufferFreed(this.size());
        }
    }

    public int getHandle() {
        return this.handle;
    }

    public static String usageToString(int usageIn) {
        StringBuilder stringbuilder = new StringBuilder();
        if ((usageIn & 1) != 0) {
            stringbuilder.append("R");
        }

        if ((usageIn & 2) != 0) {
            stringbuilder.append("W");
        }

        if ((usageIn & 4) != 0) {
            stringbuilder.append("C");
        }

        if ((usageIn & 16) != 0) {
            stringbuilder.append("S");
        }

        if ((usageIn & 8) != 0) {
            stringbuilder.append("D");
        }

        if ((usageIn & 32) != 0) {
            stringbuilder.append("V");
        }

        if ((usageIn & 64) != 0) {
            stringbuilder.append("I");
        }

        if ((usageIn & 128) != 0) {
            stringbuilder.append("U");
        }

        if ((usageIn & 256) != 0) {
            stringbuilder.append("T");
        }

        if (stringbuilder.isEmpty()) {
            stringbuilder.append("0x" + Integer.toHexString(usageIn));
        }

        return stringbuilder.toString();
    }

    @Override
    public String toString() {
        String s = usageToString(this.usage());
        return this.label.get() + ", " + s + ", " + this.size();
    }

    public static class GlMappedView implements GpuBuffer.MappedView {
        private final Runnable unmap;
        private final GlBuffer buffer;
        private final ByteBuffer data;
        private boolean closed;

        protected GlMappedView(Runnable p_410033_, GlBuffer p_409269_, ByteBuffer p_408733_) {
            this.unmap = p_410033_;
            this.buffer = p_409269_;
            this.data = p_408733_;
        }

        @Override
        public ByteBuffer data() {
            return this.data;
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                this.unmap.run();
            }
        }
    }
}
