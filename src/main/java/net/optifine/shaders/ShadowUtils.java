package net.optifine.shaders;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.optifine.util.WorldUtils;

public class ShadowUtils {
    public static Iterator<SectionRenderDispatcher.RenderSection> makeShadowChunkIterator(
        ClientLevel world, float partialTicks, Entity viewEntity, int renderDistanceChunks, ViewArea viewFrustum
    ) {
        float f = Shaders.getShadowRenderDistance();
        if (!(f <= 0.0F) && !(f >= (renderDistanceChunks - 1) * 16)) {
            int i = Mth.ceil(f / 16.0F) + 1;
            float f6 = WorldUtils.getCelestialAngleRadians(partialTicks);
            float f1 = Shaders.sunPathRotation * Mth.deg2Rad;
            float f2 = f6 > Mth.PId2 && f6 < 3.0F * Mth.PId2 ? f6 + (float) Math.PI : f6;
            float f3 = -Mth.sin(f2);
            float f4 = Mth.cos(f2) * Mth.cos(f1);
            float f5 = -Mth.cos(f2) * Mth.sin(f1);
            BlockPos blockpos = new BlockPos(
                Mth.floor(viewEntity.getX()) >> 4, Mth.floor(viewEntity.getY()) >> 4, Mth.floor(viewEntity.getZ()) >> 4
            );
            BlockPos blockpos1 = blockpos.offset((int)(-f3 * i), (int)(-f4 * i), (int)(-f5 * i));
            BlockPos blockpos2 = blockpos.offset((int)(f3 * renderDistanceChunks), (int)(f4 * renderDistanceChunks), (int)(f5 * renderDistanceChunks));
            return new IteratorRenderChunks(viewFrustum, blockpos1, blockpos2, i, i);
        } else {
            List<SectionRenderDispatcher.RenderSection> list = Arrays.asList(viewFrustum.sections);
            return list.iterator();
        }
    }
}
