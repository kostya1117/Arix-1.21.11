package net.optifine.model;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class BakedQuadRetextured {
    public static BakedQuad make(BakedQuad quad, TextureAtlasSprite spriteIn) {
        long[] along = quad.getPackedUVs();

        for (int i = 0; i < along.length; i++) {
            along[i] = remapUv(quad.packedUV(i), quad.sprite(), spriteIn);
        }

        return quad.makeCopy(along, spriteIn);
    }

    private static long remapUv(long pUv, TextureAtlasSprite sprite, TextureAtlasSprite spriteNew) {
        float f = UVPair.unpackU(pUv);
        float f1 = UVPair.unpackV(pUv);
        float f2 = spriteNew.getInterpolatedU16(sprite.getUnInterpolatedU16(f));
        float f3 = spriteNew.getInterpolatedV16(sprite.getUnInterpolatedV16(f1));
        return UVPair.pack(f2, f3);
    }
}
