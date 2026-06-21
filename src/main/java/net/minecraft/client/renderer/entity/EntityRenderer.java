package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;

import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.common.util.Result;
import net.optifine.Config;
import net.optifine.DynamicLights;
import net.optifine.entity.model.IEntityRenderer;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import net.optifine.util.Either;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Esp;

public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> implements IEntityRenderer, EntityRendererInter<T> {
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    public static final float NAMETAG_SCALE = 0.025F;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    public float shadowRadius;
    public float shadowStrength = 1.0F;
    private EntityType entityType = null;
    private Identifier locationTextureCustom = null;
    public float shadowOffsetX;
    public float shadowOffsetZ;
    public float leashOffsetX;
    public float leashOffsetY;
    public float leashOffsetZ;

    protected EntityRenderer(EntityRendererProvider.Context p_174008_) {
        this.entityRenderDispatcher = p_174008_.getEntityRenderDispatcher();
        this.font = p_174008_.getFont();
    }

    public final int getPackedLightCoords(T p_114506_, float p_114507_) {
        BlockPos blockpos = BlockPos.containing(p_114506_.getLightProbePosition(p_114507_));
        int i = LightTexture.pack(this.getBlockLightLevel(p_114506_, blockpos), this.getSkyLightLevel(p_114506_, blockpos));
        if (Config.isDynamicLights()) {
            i = DynamicLights.getCombinedLight(p_114506_, i);
        }

        return i;
    }

    protected int getSkyLightLevel(T p_114509_, BlockPos p_114510_) {
        return p_114509_.level().getBrightness(LightLayer.SKY, p_114510_);
    }

    protected int getBlockLightLevel(T p_114496_, BlockPos p_114497_) {
        return p_114496_.isOnFire() ? 15 : p_114496_.level().getBrightness(LightLayer.BLOCK, p_114497_);
    }

    public boolean shouldRender(T p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        if (!p_114491_.shouldRender(p_114493_, p_114494_, p_114495_)) {
            return false;
        }

        if (!this.affectedByCulling(p_114491_)) {
            return true;
        }

        AABB aabb = this.getBoundingBoxForCulling(p_114491_).inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = new AABB(
                p_114491_.getX() - 2.0,
                p_114491_.getY() - 2.0,
                p_114491_.getZ() - 2.0,
                p_114491_.getX() + 2.0,
                p_114491_.getY() + 2.0,
                p_114491_.getZ() + 2.0
            );
        }

        if (p_114492_.isVisible(aabb)) {
            return true;
        }

        if (p_114491_ instanceof Leashable leashable) {
            Entity entity = leashable.getLeashHolder();
            if (entity != null) {
                AABB aabb1 = this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity);
                return p_114492_.isVisible(aabb1) || p_114492_.isVisible(aabb.minmax(aabb1));
            }
        }

        return false;
    }

    protected AABB getBoundingBoxForCulling(T p_365369_) {
        return p_365369_.getBoundingBox();
    }

    protected boolean affectedByCulling(T p_366877_) {
        return true;
    }

    public Vec3 getRenderOffset(S p_367733_) {
        return p_367733_.passengerOffset != null ? p_367733_.passengerOffset : Vec3.ZERO;
    }

    public void submit(S p_431602_, PoseStack p_427228_, SubmitNodeCollector p_425204_, CameraRenderState p_426406_) {
        if (p_431602_.leashStates != null) {
            for (EntityRenderState.LeashState entityrenderstate$leashstate : p_431602_.leashStates) {
                p_425204_.submitLeash(p_427228_, entityrenderstate$leashstate);
            }
        }

        this.submitNameTag(p_431602_, p_427228_, p_425204_, p_426406_);
    }

    protected boolean shouldShowName(T entity, double distance) {
        if (Arix.getInstance()
                .getModuleRepo()
                .getModule(Esp.class)
                .isState()) {
            return false;
        }

        return entity.shouldShowName() ||
                entity.hasCustomName() &&
                        entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    public Font getFont() {
        return this.font;
    }

    protected void submitNameTag(S p_429896_, PoseStack p_428845_, SubmitNodeCollector p_426439_, CameraRenderState p_428408_) {
        if (Arix.getInstance()
                .getModuleRepo()
                .getModule(Esp.class)
                .isState()) {
            return;
        }
        boolean flag = p_429896_.nameTag != null;
        Component component = p_429896_.nameTag;
        if (Reflector.ForgeEventFactoryClient_fireRenderNameTagEvent.exists()) {
            HasResult hasresult = (HasResult)Reflector.ForgeEventFactoryClient_fireRenderNameTagEvent
                .call(p_429896_, p_429896_.nameTag, this, p_428845_, p_426439_, p_428408_);
            Result result = hasresult.getResult();
            if (result != Result.DENY && (result == Result.ALLOW || p_429896_.nameTag != null)) {
                flag = true;
                component = (Component)Reflector.call(hasresult, Reflector.RenderNameTagEvent_getContent);
            }
        }

        if (flag) {
            p_426439_.submitNameTag(p_428845_, p_429896_.nameTagAttachment, 0, component, !p_429896_.isDiscrete, p_429896_.lightCoords, p_429896_.distanceToCameraSq, p_428408_);
        }
    }

    protected  Component getNameTag(T p_361489_) {
        return p_361489_.getDisplayName();
    }

    protected float getShadowRadius(S p_364114_) {
        return this.shadowRadius;
    }

    protected float getShadowStrength(S p_376038_) {
        return this.shadowStrength;
    }

    public abstract S createRenderState();

    public final S createRenderState(T p_363266_, float p_363950_) {
        S s = this.createRenderState();
        this.extractRenderState(p_363266_, s, p_363950_);
        this.finalizeRenderState(p_363266_, s);
        return s;
    }

    public void extractRenderState(T p_367571_, S p_367427_, float p_363243_) {
        p_367427_.entityType = p_367571_.getType();
        p_367427_.x = Mth.lerp(p_363243_, p_367571_.xOld, p_367571_.getX());
        p_367427_.y = Mth.lerp(p_363243_, p_367571_.yOld, p_367571_.getY());
        p_367427_.z = Mth.lerp(p_363243_, p_367571_.zOld, p_367571_.getZ());
        p_367427_.isInvisible = p_367571_.isInvisible();
        p_367427_.ageInTicks = p_367571_.tickCount + p_363243_;
        p_367427_.boundingBoxWidth = p_367571_.getBbWidth();
        p_367427_.boundingBoxHeight = p_367571_.getBbHeight();
        p_367427_.eyeHeight = p_367571_.getEyeHeight();
        if (p_367571_.isPassenger()
            && p_367571_.getVehicle() instanceof AbstractMinecart abstractminecart
            && abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
            && newminecartbehavior.cartHasPosRotLerp()) {
            double d2 = Mth.lerp(p_363243_, abstractminecart.xOld, abstractminecart.getX());
            double d0 = Mth.lerp(p_363243_, abstractminecart.yOld, abstractminecart.getY());
            double d1 = Mth.lerp(p_363243_, abstractminecart.zOld, abstractminecart.getZ());
            p_367427_.passengerOffset = newminecartbehavior.getCartLerpPosition(p_363243_).subtract(new Vec3(d2, d0, d1));
        } else {
            p_367427_.passengerOffset = null;
        }

        if (this.entityRenderDispatcher.camera != null) {
            p_367427_.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(p_367571_);

            boolean nametagsEnabled = Arix.getInstance()
                    .getModuleRepo()
                    .getModule(Esp.class)
                    .isState();

            boolean flag1 = !nametagsEnabled && p_367427_.distanceToCameraSq < 4096.0
                    && this.shouldShowName(p_367571_, p_367427_.distanceToCameraSq);

            if (Reflector.ForgeHooksClient_isNameplateInRenderDistance.exists()) {
                flag1 = !nametagsEnabled
                        && Reflector.ForgeHooksClient_isNameplateInRenderDistance.callBoolean(p_367571_, p_367427_.distanceToCameraSq)
                        && this.shouldShowName(p_367571_, p_367427_.distanceToCameraSq);
            }

            if (flag1) {
                p_367427_.nameTag = this.getNameTag(p_367571_);
                p_367427_.nameTagAttachment = p_367571_.getAttachments()
                        .getNullable(EntityAttachment.NAME_TAG, 0, p_367571_.getYRot(p_363243_));
            } else {
                p_367427_.nameTag = null;
                p_367427_.nameTagAttachment = null;
            }
        }

        label91: {
            p_367427_.isDiscrete = p_367571_.isDiscrete();
            Level level = p_367571_.level();
            if (p_367571_ instanceof Leashable leashable) {
                Entity entity = leashable.getLeashHolder();
                if (entity instanceof Entity) {
                    float f = p_367571_.getPreciseBodyRotation(p_363243_) * (float) (Math.PI / 180.0);
                    Vec3 vec31 = leashable.getLeashOffset(p_363243_);
                    if (this.leashOffsetX != 0.0F || this.leashOffsetY != 0.0F || this.leashOffsetZ != 0.0F) {
                        vec31 = vec31.add(this.leashOffsetX, this.leashOffsetY, this.leashOffsetZ);
                    }

                    BlockPos blockpos = BlockPos.containing(p_367571_.getEyePosition(p_363243_));
                    BlockPos blockpos1 = BlockPos.containing(entity.getEyePosition(p_363243_));
                    int i = this.getBlockLightLevel(p_367571_, blockpos);
                    int j = this.entityRenderDispatcher.getRenderer(entity).getBlockLightLevel(entity, blockpos1);
                    if (Config.isDynamicLights()) {
                        int k = LightTexture.pack(i, 0);
                        int l = LightTexture.pack(j, 0);
                        k = DynamicLights.getCombinedLight(p_367571_, k);
                        l = DynamicLights.getCombinedLight(entity, l);
                        i = LightTexture.block(k);
                        j = LightTexture.block(l);
                    }

                    int l1 = level.getBrightness(LightLayer.SKY, blockpos);
                    int i2 = level.getBrightness(LightLayer.SKY, blockpos1);
                    boolean flag = entity.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
                    int i1 = flag ? 4 : 1;
                    if (p_367427_.leashStates == null || p_367427_.leashStates.size() != i1) {
                        p_367427_.leashStates = new ArrayList<>(i1);

                        for (int j1 = 0; j1 < i1; j1++) {
                            p_367427_.leashStates.add(new EntityRenderState.LeashState());
                        }
                    }

                    if (flag) {
                        float f1 = entity.getPreciseBodyRotation(p_363243_) * (float) (Math.PI / 180.0);
                        Vec3 vec3 = entity.getPosition(p_363243_);
                        Vec3[] avec3 = leashable.getQuadLeashOffsets();
                        Vec3[] avec31 = entity.getQuadLeashHolderOffsets();
                        int k1 = 0;

                        while (true) {
                            if (k1 >= i1) {
                                break label91;
                            }

                            EntityRenderState.LeashState entityrenderstate$leashstate = p_367427_.leashStates.get(k1);
                            entityrenderstate$leashstate.offset = avec3[k1].yRot(-f);
                            entityrenderstate$leashstate.start = p_367571_.getPosition(p_363243_).add(entityrenderstate$leashstate.offset);
                            entityrenderstate$leashstate.end = vec3.add(avec31[k1].yRot(-f1));
                            entityrenderstate$leashstate.startBlockLight = i;
                            entityrenderstate$leashstate.endBlockLight = j;
                            entityrenderstate$leashstate.startSkyLight = l1;
                            entityrenderstate$leashstate.endSkyLight = i2;
                            entityrenderstate$leashstate.slack = false;
                            k1++;
                        }
                    } else {
                        Vec3 vec32 = vec31.yRot(-f);
                        EntityRenderState.LeashState entityrenderstate$leashstate1 = (EntityRenderState.LeashState)p_367427_.leashStates.getFirst();
                        entityrenderstate$leashstate1.offset = vec32;
                        entityrenderstate$leashstate1.start = p_367571_.getPosition(p_363243_).add(vec32);
                        entityrenderstate$leashstate1.end = entity.getRopeHoldPosition(p_363243_);
                        entityrenderstate$leashstate1.startBlockLight = i;
                        entityrenderstate$leashstate1.endBlockLight = j;
                        entityrenderstate$leashstate1.startSkyLight = l1;
                        entityrenderstate$leashstate1.endSkyLight = i2;
                        break label91;
                    }
                }
            }

            p_367427_.leashStates = null;
        }

        p_367427_.displayFireAnimation = p_367571_.displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag2 = minecraft.shouldEntityAppearGlowing(p_367571_);
        p_367427_.outlineColor = flag2 ? ARGB.opaque(p_367571_.getTeamColor()) : 0;
        p_367427_.lightCoords = this.getPackedLightCoords(p_367571_, p_363243_);
        p_367427_.entity = p_367571_;
    }

    protected void finalizeRenderState(T p_430394_, S p_428267_) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = p_430394_.level();
        this.extractShadow(p_428267_, minecraft, level);
    }

    private void extractShadow(S p_431467_, Minecraft p_425570_, Level p_429120_) {
        p_431467_.shadowPieces.clear();
        if (!Config.isShaders() || !Shaders.shouldSkipDefaultShadow) {
            if (p_425570_.options.entityShadows().get() && !p_431467_.isInvisible) {
                float f = Math.min(this.getShadowRadius(p_431467_), 32.0F);
                p_431467_.shadowRadius = f;
                if (f > 0.0F) {
                    double d0 = p_431467_.distanceToCameraSq;
                    float f1 = (float)((1.0 - d0 / 256.0) * this.getShadowStrength(p_431467_));
                    if (f1 > 0.0F) {
                        int i = Mth.floor(p_431467_.x - f);
                        int j = Mth.floor(p_431467_.x + f);
                        int k = Mth.floor(p_431467_.z - f);
                        int l = Mth.floor(p_431467_.z + f);
                        float f2 = Math.min(f1 / 0.5F - 1.0F, f);
                        int i1 = Mth.floor(p_431467_.y - f2);
                        int j1 = Mth.floor(p_431467_.y);
                        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                        for (int k1 = k; k1 <= l; k1++) {
                            for (int l1 = i; l1 <= j; l1++) {
                                blockpos$mutableblockpos.set(l1, 0, k1);
                                ChunkAccess chunkaccess = p_429120_.getChunk(blockpos$mutableblockpos);

                                for (int i2 = i1; i2 <= j1; i2++) {
                                    blockpos$mutableblockpos.setY(i2);
                                    this.extractShadowPiece(p_431467_, p_429120_, f1, blockpos$mutableblockpos, chunkaccess);
                                }
                            }
                        }
                    }
                }
            } else {
                p_431467_.shadowRadius = 0.0F;
            }
        }
    }

    private void extractShadowPiece(S p_428100_, Level p_431705_, float p_429996_, BlockPos.MutableBlockPos p_424981_, ChunkAccess p_426623_) {
        float f = p_429996_ - (float)(p_428100_.y - p_424981_.getY()) * 0.5F;
        BlockPos blockpos = p_424981_.below();
        BlockState blockstate = p_426623_.getBlockState(blockpos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            int i = p_431705_.getMaxLocalRawBrightness(p_424981_);
            if (i > 3 && blockstate.isCollisionShapeFullBlock(p_426623_, blockpos)) {
                VoxelShape voxelshape = blockstate.getShape(p_426623_, blockpos);
                if (!voxelshape.isEmpty()) {
                    float f1 = Mth.clamp(f * 0.5F * LightTexture.getBrightness(p_431705_.dimensionType(), i), 0.0F, 1.0F);
                    float f2 = (float)(p_424981_.getX() - p_428100_.x);
                    float f3 = (float)(p_424981_.getY() - p_428100_.y);
                    float f4 = (float)(p_424981_.getZ() - p_428100_.z);
                    p_428100_.shadowPieces.add(new EntityRenderState.ShadowPiece(f2, f3, f4, voxelshape, f1));
                }
            }
        }
    }

    private static  Entity getServerSideEntity(Entity p_397464_) {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
        if (integratedserver != null) {
            ServerLevel serverlevel = integratedserver.getLevel(p_397464_.level().dimension());
            if (serverlevel != null) {
                return serverlevel.getEntity(p_397464_.getId());
            }
        }

        return null;
    }

    @Override
    public Either<EntityType, BlockEntityType> getType() {
        return this.entityType == null ? null : Either.makeLeft(this.entityType);
    }

    @Override
    public void setType(Either<EntityType, BlockEntityType> type) {
        this.entityType = type.getLeft().get();
    }

    @Override
    public void setShadowSize(float shadowSize) {
        this.shadowRadius = shadowSize;
    }

    @Override
    public Identifier getLocationTextureCustom() {
        return this.locationTextureCustom;
    }

    @Override
    public void setLocationTextureCustom(Identifier locationTextureCustom) {
        this.locationTextureCustom = locationTextureCustom;
    }
    @Override
    public boolean shadowShouldShowName(T entity) {
        //? if <= 1.21.1 {
        /*
         return shouldShowName(entity);
        *///? } else {

        return ((EntityRenderer) (Object) this).createRenderState(entity, 0).nameTag != null;
        //? }
    }

    @Override
    public void shadowRenderNameTag(T entity, Component component, PoseStack poseStack,
                                    MultiBufferSource multiBufferSource, int light, float delta) {
        //? if >= 1.21.9 {

        //? } else if >= 1.21.2 {
        /*
         renderNameTag(((EntityRenderer) (Object) this).createRenderState(entity, delta), component, poseStack,
                multiBufferSource, light);
        *///? } else if >= 1.20.5 {
        /*
         renderNameTag(entity, component, poseStack, multiBufferSource, light, delta);
        *///? } else {
        /*
         renderNameTag(entity, component, poseStack, multiBufferSource, light);
        *///? }
    }

    //? if <= 1.21.1 {
    /*
     @Shadow
     public abstract boolean shouldShowName(T entity);
    *///? }

    //? if < 1.21.9 {
    /*
     //? if >= 1.21.2 {

         @Shadow
         public abstract void renderNameTag(net.minecraft.client.renderer.entity.state.EntityRenderState entityRenderState,
                 Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i);
     //? } else if >= 1.20.5 {
    /^
         @Shadow
      public abstract void renderNameTag(T entity, Component component, PoseStack poseStack,
      MultiBufferSource multiBufferSource, int i, float f);
     ^///? } else {
    /^
         @Shadow
         public abstract void renderNameTag(T entity, Component component, PoseStack poseStack,
      MultiBufferSource multiBufferSource, int i);
     ^///? }
    *///? }

    @Override
    public boolean entityCullingIgnoresCulling(T entity) {
        //? if <= 1.21.1 {
        /*
         return entity.noCulling;
        *///? } else {

        return !affectedByCulling(entity);
        //? }
    }

    @Override
    public AABB entityCullingGetCullingBox(T entity) {
        //? if <= 1.21.1 {
        /*
         return entity.getBoundingBoxForCulling();
        *///? } else {

        return getBoundingBoxForCulling(entity);
        //? }
    }
}
