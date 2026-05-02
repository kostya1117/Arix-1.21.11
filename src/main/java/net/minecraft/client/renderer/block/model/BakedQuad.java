package net.minecraft.client.renderer.block.model;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.optifine.model.BakedQuadRetextured;
import net.optifine.model.PositionUv;
import net.optifine.model.QuadBounds;
import net.optifine.render.QuadProperties;
import net.optifine.render.QuadVertexPositions;
import net.optifine.render.VertexPosition;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record BakedQuad(
    Vector3fc position0,
    Vector3fc position1,
    Vector3fc position2,
    Vector3fc position3,
    long packedUV0,
    long packedUV1,
    long packedUV2,
    long packedUV3,
    int tintIndex,
    Direction direction,
    TextureAtlasSprite sprite,
    boolean shade,
    int lightEmission,
    boolean ambientOcclusion,
    QuadProperties properties
) {
    public static final int VERTEX_COUNT = 4;

    public BakedQuad(
        Vector3fc position0,
        Vector3fc position1,
        Vector3fc position2,
        Vector3fc position3,
        long packedUV0,
        long packedUV1,
        long packedUV2,
        long packedUV3,
        int tintIndex,
        Direction face,
        TextureAtlasSprite sprite,
        boolean shade,
        int lightEmission,
        boolean ambientOcclusion
    ) {
        this(
            position0,
            position1,
            position2,
            position3,
            packedUV0,
            packedUV1,
            packedUV2,
            packedUV3,
            tintIndex,
            fixFace(face, position0, position1, position2, position3),
            fixSprite(sprite, packedUV0, packedUV1, packedUV2, packedUV3),
            shade,
            lightEmission,
            ambientOcclusion,
            new QuadProperties()
        );
    }

    public BakedQuad(
        Vector3fc position0,
        Vector3fc position1,
        Vector3fc position2,
        Vector3fc position3,
        long packedUV0,
        long packedUV1,
        long packedUV2,
        long packedUV3,
        int tintIndex,
        Direction direction,
        TextureAtlasSprite sprite,
        boolean shade,
        int lightEmission
    ) {
        this(position0, position1, position2, position3, packedUV0, packedUV1, packedUV2, packedUV3, tintIndex, direction, sprite, shade, lightEmission, true);
    }

    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    public Vector3fc position(int p_459790_) {
        return switch (p_459790_) {
            case 0 -> this.position0;
            case 1 -> this.position1;
            case 2 -> this.position2;
            case 3 -> this.position3;
            default -> throw new IndexOutOfBoundsException(p_459790_);
        };
    }

    public long packedUV(int p_455420_) {
        return switch (p_455420_) {
            case 0 -> this.packedUV0;
            case 1 -> this.packedUV1;
            case 2 -> this.packedUV2;
            case 3 -> this.packedUV3;
            default -> throw new IndexOutOfBoundsException(p_455420_);
        };
    }

    public BakedQuad makeCopy(PositionUv[] posUvs, TextureAtlasSprite spriteIn) {
        if (posUvs.length != 4) {
            throw new IllegalArgumentException("Invalid vertex data length: " + posUvs.length);
        }

        Vector3f vector3f = posUvs[0].position();
        Vector3f vector3f1 = posUvs[1].position();
        Vector3f vector3f2 = posUvs[2].position();
        Vector3f vector3f3 = posUvs[3].position();
        long i = posUvs[0].packedUv();
        long j = posUvs[1].packedUv();
        long k = posUvs[2].packedUv();
        long l = posUvs[3].packedUv();
        return new BakedQuad(
            vector3f,
            vector3f1,
            vector3f2,
            vector3f3,
            i,
            j,
            k,
            l,
            this.tintIndex,
            this.direction,
            spriteIn,
            this.shade,
            this.lightEmission,
            this.ambientOcclusion
        );
    }

    public BakedQuad makeCopy(long[] uvs, TextureAtlasSprite spriteIn) {
        if (uvs.length != 4) {
            throw new IllegalArgumentException("Invalid vertex data length: " + uvs.length);
        }

        long i = uvs[0];
        long j = uvs[1];
        long k = uvs[2];
        long l = uvs[3];
        return new BakedQuad(
            this.position0,
            this.position1,
            this.position2,
            this.position3,
            i,
            j,
            k,
            l,
            this.tintIndex,
            this.direction,
            spriteIn,
            this.shade,
            this.lightEmission,
            this.ambientOcclusion
        );
    }

    public BakedQuad makeCopyReverse() {
        Vector3fc vector3fc = this.position3;
        Vector3fc vector3fc1 = this.position2;
        Vector3fc vector3fc2 = this.position1;
        Vector3fc vector3fc3 = this.position0;
        long i = this.packedUV3;
        long j = this.packedUV2;
        long k = this.packedUV1;
        long l = this.packedUV0;
        return new BakedQuad(
            vector3fc,
            vector3fc1,
            vector3fc2,
            vector3fc3,
            i,
            j,
            k,
            l,
            this.tintIndex,
            this.direction,
            this.sprite,
            this.shade,
            this.lightEmission,
            this.ambientOcclusion
        );
    }

    public long[] getPackedUVs() {
        return new long[]{this.packedUV0, this.packedUV1, this.packedUV2, this.packedUV3};
    }

    private static Direction fixFace(Direction faceIn, Vector3fc pos0, Vector3fc pos1, Vector3fc pos2, Vector3fc pos3) {
        if (faceIn == null) {
            faceIn = FaceBakery.calculateFacing(new Vector3fc[]{pos0, pos1, pos2, pos3});
        }

        if (faceIn == null) {
            faceIn = Direction.UP;
        }

        return faceIn;
    }

    private static TextureAtlasSprite fixSprite(TextureAtlasSprite spriteIn, long pUV0, long pUV1, long pUV2, long pUV3) {
        return spriteIn == null ? getSpriteByUv(spriteIn.getTextureAtlas(), new long[]{pUV0, pUV1, pUV2, pUV3}) : spriteIn;
    }

    public long getPackedUvSingle(int indexIn) {
        if (this.properties.packedUvsSingle == null) {
            this.properties.packedUvsSingle = makePackedUvsSingle(new long[]{this.packedUV0, this.packedUV1, this.packedUV2, this.packedUV3}, this.sprite);
        }

        return this.properties.packedUvsSingle[indexIn];
    }

    private static long[] makePackedUvsSingle(long[] packedUVs, TextureAtlasSprite sprite) {
        long[] along = new long[packedUVs.length];

        for (int i = 0; i < 4; i++) {
            float f = UVPair.unpackU(packedUVs[i]);
            float f1 = UVPair.unpackV(packedUVs[i]);
            float f2 = sprite.toSingleU(f);
            float f3 = sprite.toSingleV(f1);
            along[i] = UVPair.pack(f2, f3);
        }

        return along;
    }

    private static TextureAtlasSprite getSpriteByUv(TextureAtlas atlasIn, long[] packedUVs) {
        float f = 1.0F;
        float f1 = 1.0F;
        float f2 = 0.0F;
        float f3 = 0.0F;

        for (int i = 0; i < 4; i++) {
            float f4 = UVPair.unpackU(packedUVs[i]);
            float f5 = UVPair.unpackV(packedUVs[i]);
            f = Math.min(f, f4);
            f1 = Math.min(f1, f5);
            f2 = Math.max(f2, f4);
            f3 = Math.max(f3, f5);
        }

        float f6 = (f + f2) / 2.0F;
        float f7 = (f1 + f3) / 2.0F;
        return atlasIn.getIconByUV(f6, f7);
    }

    public QuadBounds getQuadBounds() {
        if (this.properties.quadBounds == null) {
            this.properties.quadBounds = new QuadBounds(new Vector3fc[]{this.position0, this.position1, this.position2, this.position3});
        }

        return this.properties.quadBounds;
    }

    public float getMidX() {
        QuadBounds quadbounds = this.getQuadBounds();
        return (quadbounds.getMaxX() + quadbounds.getMinX()) / 2.0F;
    }

    public double getMidY() {
        QuadBounds quadbounds = this.getQuadBounds();
        return (quadbounds.getMaxY() + quadbounds.getMinY()) / 2.0F;
    }

    public double getMidZ() {
        QuadBounds quadbounds = this.getQuadBounds();
        return (quadbounds.getMaxZ() + quadbounds.getMinZ()) / 2.0F;
    }

    public boolean isFaceQuad() {
        QuadBounds quadbounds = this.getQuadBounds();
        return quadbounds.isFaceQuad(this.direction);
    }

    public boolean isFullQuad() {
        QuadBounds quadbounds = this.getQuadBounds();
        return quadbounds.isFullQuad(this.direction);
    }

    public boolean isFullFaceQuad() {
        return this.isFullQuad() && this.isFaceQuad();
    }

    public BakedQuad getQuadEmissive() {
        if (this.properties.quadEmissiveChecked) {
            return this.properties.quadEmissive;
        }

        if (this.properties.quadEmissive == null && this.sprite != null && this.sprite.spriteEmissive != null) {
            this.properties.quadEmissive = BakedQuadRetextured.make(this, this.sprite.spriteEmissive);
        }

        this.properties.quadEmissiveChecked = true;
        return this.properties.quadEmissive;
    }

    public VertexPosition[] getVertexPositions(int key) {
        if (this.properties.quadVertexPositions == null) {
            this.properties.quadVertexPositions = new QuadVertexPositions();
        }

        return this.properties.quadVertexPositions.get(key);
    }

    @Override
    public String toString() {
        return "tint: " + this.tintIndex + ", facing: " + this.direction + ", sprite: " + this.sprite;
    }
}
