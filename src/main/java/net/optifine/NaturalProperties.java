package net.optifine;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class NaturalProperties {
    public int rotation = 1;
    public boolean flip = false;
    private Map[] quadMaps = new Map[8];

    public NaturalProperties(String type) {
        if (type.equals("4")) {
            this.rotation = 4;
        } else if (type.equals("2")) {
            this.rotation = 2;
        } else if (type.equals("F")) {
            this.flip = true;
        } else if (type.equals("4F")) {
            this.rotation = 4;
            this.flip = true;
        } else if (type.equals("2F")) {
            this.rotation = 2;
            this.flip = true;
        } else {
            Config.warn("NaturalTextures: Unknown type: " + type);
        }
    }

    public boolean isValid() {
        return this.rotation == 2 || this.rotation == 4 ? true : this.flip;
    }

    public synchronized BakedQuad getQuad(BakedQuad quadIn, int rotate, boolean flipU) {
        int i = rotate;
        if (flipU) {
            i |= 4;
        }

        if (i > 0 && i < this.quadMaps.length) {
            Map map = this.quadMaps[i];
            if (map == null) {
                map = new IdentityHashMap(1);
                this.quadMaps[i] = map;
            }

            BakedQuad bakedquad = (BakedQuad)map.get(quadIn);
            if (bakedquad == null) {
                bakedquad = this.makeQuad(quadIn, rotate, flipU);
                map.put(quadIn, bakedquad);
            }

            return bakedquad;
        } else {
            return quadIn;
        }
    }

    private BakedQuad makeQuad(BakedQuad quad, int rotate, boolean flipU) {
        int i = quad.tintIndex();
        Direction direction = quad.direction();
        TextureAtlasSprite textureatlassprite = quad.sprite();
        boolean flag = quad.shade();
        int j = quad.lightEmission();
        if (!this.isFullSprite(quad)) {
            rotate = 0;
        }

        long[] along = this.transformVertexData(quad.getPackedUVs(), rotate, flipU);
        return quad.makeCopy(along, quad.sprite());
    }

    private long[] transformVertexData(long[] uvs, int rotate, boolean flipU) {
        long[] along = (long[])uvs.clone();
        int i = 4 - rotate;
        if (flipU) {
            i += 3;
        }

        i %= 4;

        for (int j = 0; j < 4; j++) {
            along[i] = uvs[j];
            if (flipU) {
                if (--i < 0) {
                    i = 3;
                }
            } else if (++i > 3) {
                i = 0;
            }
        }

        return along;
    }

    private boolean isFullSprite(BakedQuad quad) {
        TextureAtlasSprite textureatlassprite = quad.sprite();
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = f1 - f;
        float f3 = f2 / 256.0F;
        float f4 = textureatlassprite.getV0();
        float f5 = textureatlassprite.getV1();
        float f6 = f5 - f4;
        float f7 = f6 / 256.0F;

        for (int i = 0; i < 4; i++) {
            float f8 = UVPair.unpackU(quad.packedUV(i));
            float f9 = UVPair.unpackV(quad.packedUV(i));
            if (!this.equalsDelta(f8, f, f3) && !this.equalsDelta(f8, f1, f3)) {
                return false;
            }

            if (!this.equalsDelta(f9, f4, f7) && !this.equalsDelta(f9, f5, f7)) {
                return false;
            }
        }

        return true;
    }

    private boolean equalsDelta(float x1, float x2, float deltaMax) {
        float f = Mth.abs(x1 - x2);
        return f < deltaMax;
    }
}
