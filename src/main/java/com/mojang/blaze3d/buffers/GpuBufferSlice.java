package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.opengl.GlBuffer;
import net.optifine.util.Mutable;

@DontObfuscate
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length, Mutable<Object> mutableData) {
    public GpuBufferSlice(GpuBuffer buffer, long offset, long length) {
        this(buffer, offset, length, new Mutable<>());
    }

    public GpuBufferSlice slice(long p_457630_, long p_451935_) {
        if (p_457630_ >= 0L && p_451935_ >= 0L && p_457630_ + p_451935_ <= this.length) {
            return new GpuBufferSlice(this.buffer, this.offset + p_457630_, p_451935_);
        } else {
            throw new IllegalArgumentException(
                "Offset of "
                    + p_457630_
                    + " and length "
                    + p_451935_
                    + " would put new slice outside existing slice's range (of "
                    + this.offset
                    + ","
                    + this.length
                    + ")"
            );
        }
    }

    public GlBuffer glBuffer() {
        return (GlBuffer)this.buffer;
    }

    public Object getData() {
        return this.mutableData.get();
    }

    public void setData(Object data) {
        this.mutableData.set(data);
    }
}
