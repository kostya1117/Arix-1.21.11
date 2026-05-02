package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@DontObfuscate
public interface GpuFence extends AutoCloseable {
    @Override
    void close();

    boolean awaitCompletion(long p_368162_);
}
