package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeVertexConsumer;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Matrix3f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public interface VertexConsumer extends IForgeVertexConsumer {
    ThreadLocal<RenderEnv> RENDER_ENV = ThreadLocal.withInitial(() -> new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0)));
    boolean FORGE = Reflector.ForgeHooksClient.exists();

    default RenderEnv getRenderEnv(BlockState blockState, BlockPos blockPos) {
        RenderEnv renderenv = RENDER_ENV.get();
        renderenv.reset(blockState, blockPos);
        return renderenv;
    }

    VertexConsumer addVertex(float p_344294_, float p_342213_, float p_344859_);

    VertexConsumer setColor(int p_342749_, int p_344324_, int p_343336_, int p_342831_);

    VertexConsumer setColor(int p_345390_);

    VertexConsumer setUv(float p_344155_, float p_345269_);

    VertexConsumer setUv1(int p_344168_, int p_342818_);

    VertexConsumer setUv2(int p_342773_, int p_345341_);

    VertexConsumer setNormal(float p_342733_, float p_342268_, float p_344916_);

    VertexConsumer setLineWidth(float p_459353_);

    default void addVertex(
        float p_342335_,
        float p_342594_,
        float p_342395_,
        int p_344436_,
        float p_344317_,
        float p_344558_,
        int p_344862_,
        int p_343109_,
        float p_343232_,
        float p_342995_,
        float p_343739_
    ) {
        this.addVertex(p_342335_, p_342594_, p_342395_);
        this.setColor(p_344436_);
        this.setUv(p_344317_, p_344558_);
        this.setOverlay(p_344862_);
        this.setLight(p_343109_);
        this.setNormal(p_343232_, p_342995_, p_343739_);
    }

    default VertexConsumer setColor(float p_345344_, float p_343040_, float p_343668_, float p_342740_) {
        return this.setColor((int)(p_345344_ * 255.0F), (int)(p_343040_ * 255.0F), (int)(p_343668_ * 255.0F), (int)(p_342740_ * 255.0F));
    }

    default VertexConsumer setLight(int p_342385_) {
        return this.setUv2(p_342385_ & 65535, p_342385_ >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int p_345433_) {
        return this.setUv1(p_345433_ & 65535, p_345433_ >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose p_85996_, BakedQuad p_85997_, float p_85999_, float p_86000_, float p_86001_, float p_330684_, int p_86003_, int p_332867_
    ) {
        this.putBulkData(
            p_85996_,
            p_85997_,
            this.getTempFloat4(1.0F, 1.0F, 1.0F, 1.0F),
            p_85999_,
            p_86000_,
            p_86001_,
            p_330684_,
            this.getTempInt4(p_86003_, p_86003_, p_86003_, p_86003_),
            p_332867_
        );
    }

    default void putBulkData(
        PoseStack.Pose p_85988_,
        BakedQuad p_85989_,
        float[] p_331915_,
        float p_85990_,
        float p_85991_,
        float p_85992_,
        float p_335371_,
        int[] p_331444_,
        int p_85993_
    ) {
        this.putSprite(p_85989_.sprite());
        boolean flag = ModelBlockRenderer.isSeparateAoLightValue();
        Vector3fc vector3fc = p_85989_.direction().getUnitVec3f();
        Matrix4f matrix4f = p_85988_.pose();
        Vector3f vector3f = p_85988_.transformNormal(vector3fc, this.getTempVec3f());
        float f = vector3f.x;
        float f1 = vector3f.y;
        float f2 = vector3f.z;
        boolean flag1 = Config.isShaders() && Shaders.useVelocityAttrib && Config.isMinecraftThread();
        if (flag1) {
            IRandomEntity irandomentity = RandomEntities.getRandomEntityRendered();
            if (irandomentity != null) {
                VertexPosition[] avertexposition = p_85989_.getVertexPositions(irandomentity.getId());
                this.setQuadVertexPositions(avertexposition);
            }
        }

        int l = p_85989_.lightEmission();

        for (int i1 = 0; i1 < 4; i1++) {
            Vector3fc vector3fc1 = p_85989_.position(i1);
            long i = this.isMultiTexture() ? p_85989_.getPackedUvSingle(i1) : p_85989_.packedUV(i1);
            float f3 = p_331915_[i1];
            if (flag) {
                p_335371_ = p_331915_[i1];
                f3 = 1.0F;
            }

            int j = ARGB.colorFromFloat(p_335371_, f3 * p_85990_, f3 * p_85991_, f3 * p_85992_);
            int k = LightTexture.lightCoordsWithEmission(p_331444_[i1], l);
            float f4 = MathUtils.getTransformX(matrix4f, vector3fc1.x(), vector3fc1.y(), vector3fc1.z());
            float f5 = MathUtils.getTransformY(matrix4f, vector3fc1.x(), vector3fc1.y(), vector3fc1.z());
            float f6 = MathUtils.getTransformZ(matrix4f, vector3fc1.x(), vector3fc1.y(), vector3fc1.z());
            float f7 = UVPair.unpackU(i);
            float f8 = UVPair.unpackV(i);
            this.addVertex(f4, f5, f6, j, f7, f8, p_85993_, k, vector3f.x(), vector3f.y(), vector3f.z());
        }
    }

    default VertexConsumer addVertex(Vector3fc p_451019_) {
        return this.addVertex(p_451019_.x(), p_451019_.y(), p_451019_.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose p_343718_, Vector3f p_344795_) {
        return this.addVertex(p_343718_, p_344795_.x(), p_344795_.y(), p_344795_.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose p_343203_, float p_343315_, float p_342573_, float p_344986_) {
        return this.addVertex(p_343203_.pose(), p_343315_, p_342573_, p_344986_);
    }

    default VertexConsumer addVertex(Matrix4fc p_460886_, float p_342636_, float p_342677_, float p_343814_) {
        Vector3f vector3f = p_460886_.transformPosition(p_342636_, p_342677_, p_343814_, this.getTempVec3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer addVertexWith2DPose(Matrix3x2fc p_460275_, float p_406462_, float p_406232_) {
        Vector2f vector2f = p_460275_.transformPosition(p_406462_, p_406232_, new Vector2f());
        return this.addVertex(vector2f.x(), vector2f.y(), 0.0F);
    }

    default VertexConsumer setNormal(PoseStack.Pose p_343706_, float p_345121_, float p_344892_, float p_344341_) {
        Vector3f vector3f = p_343706_.transformNormal(p_345121_, p_344892_, p_344341_, this.getTempVec3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose p_369767_, Vector3f p_366727_) {
        return this.setNormal(p_369767_, p_366727_.x(), p_366727_.y(), p_366727_.z());
    }

    default void putSprite(TextureAtlasSprite sprite) {
    }

    default void setSprite(TextureAtlasSprite sprite) {
    }

    default boolean isMultiTexture() {
        return false;
    }

    default RenderType getRenderType() {
        return null;
    }

    default ChunkSectionLayer getBlockLayer() {
        return null;
    }

    default Vector3f getTempVec3f() {
        return new Vector3f();
    }

    default Vector3f getTempVec3f(float x, float y, float z) {
        return this.getTempVec3f().set(x, y, z);
    }

    default Vector3f getTempVec3f(Vector3f vec) {
        return this.getTempVec3f().set(vec);
    }

    default float[] getTempFloat4(float f1, float f2, float f3, float f4) {
        return new float[]{f1, f2, f3, f4};
    }

    default int[] getTempInt4(int i1, int i2, int i3, int i4) {
        return new int[]{i1, i2, i3, i4};
    }

    default MultiBufferSource.BufferSource getRenderTypeBuffer() {
        return null;
    }

    default void setQuadVertexPositions(VertexPosition[] vps) {
    }

    default void setMidBlock(float mbx, float mby, float mbz) {
    }

    default Vector3f getMidBlock() {
        return null;
    }

    default VertexConsumer getSecondaryBuilder() {
        return null;
    }

    default int getVertexCount() {
        return 0;
    }

    default int applyBakedLighting(int lightmapCoord, int[] data, int pos) {
        int i = getLightOffset(0);
        int j = LightTexture.block(data[pos + i]);
        int k = LightTexture.sky(data[pos + i]);
        if (j == 0 && k == 0) {
            return lightmapCoord;
        }

        int l = LightTexture.block(lightmapCoord);
        int i1 = LightTexture.sky(lightmapCoord);
        l = Math.max(l, j);
        i1 = Math.max(i1, k);
        return LightTexture.pack(l, i1);
    }

    static int getLightOffset(int v) {
        return v * 8 + 6;
    }

    default Vector3f applyBakedNormals(int[] data, int pos, Matrix3f normalTransform) {
        int i = 7;
        int j = data[pos + i];
        byte b0 = (byte)(j >> 0 & 0xFF);
        byte b1 = (byte)(j >> 8 & 0xFF);
        byte b2 = (byte)(j >> 16 & 0xFF);
        if (b0 == 0 && b1 == 0 && b2 == 0) {
            return null;
        }

        Vector3f vector3f = this.getTempVec3f(b0 / 127.0F, b1 / 127.0F, b2 / 127.0F);
        MathUtils.transform(vector3f, normalTransform);
        return vector3f;
    }

    default void getBulkData(ByteBuffer buffer) {
    }

    default void putBulkData(ByteBuffer buffer) {
    }

    default boolean canAddVertexFast() {
        return false;
    }

    default void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals) {
    }
}
