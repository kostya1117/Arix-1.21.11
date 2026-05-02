package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.optifine.entity.model.CustomStaticModels;
import net.optifine.render.RenderState;
import net.optifine.util.ArrayUtils;

public class ParrotOnShoulderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private ParrotModel model;
    private ParrotModel parrotModelOriginal;

    public ParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_174511_, EntityModelSet p_174512_) {
        super(p_174511_);
        this.model = new ParrotModel(p_174512_.bakeLayer(ModelLayers.PARROT));
        this.parrotModelOriginal = this.model;
    }

    public void submit(PoseStack p_428208_, SubmitNodeCollector p_423726_, int p_424313_, AvatarRenderState p_429711_, float p_425519_, float p_431641_) {
        Parrot.Variant parrot$variant = p_429711_.parrotOnLeftShoulder;
        if (parrot$variant != null) {
            this.submitOnShoulder(p_428208_, p_423726_, p_424313_, p_429711_, parrot$variant, p_425519_, p_431641_, true);
        }

        Parrot.Variant parrot$variant1 = p_429711_.parrotOnRightShoulder;
        if (parrot$variant1 != null) {
            this.submitOnShoulder(p_428208_, p_423726_, p_424313_, p_429711_, parrot$variant1, p_425519_, p_431641_, false);
        }
    }

    private void submitOnShoulder(
        PoseStack p_431021_,
        SubmitNodeCollector p_422981_,
        int p_427224_,
        AvatarRenderState p_425167_,
        Parrot.Variant p_452298_,
        float p_428398_,
        float p_428084_,
        boolean p_431155_
    ) {
        Entity entity = p_425167_.entity;
        Entity entity1 = RenderState.getEntity();
        Entity entity2 = null;
        if (entity instanceof AbstractClientPlayer abstractclientplayer) {
            entity2 = p_431155_ ? abstractclientplayer.entityShoulderLeft : abstractclientplayer.entityShoulderRight;
            if (entity2 == null && entity2 instanceof ShoulderRidingEntity) {
                if (p_431155_) {
                    abstractclientplayer.entityShoulderLeft = (ShoulderRidingEntity)entity2;
                } else {
                    abstractclientplayer.entityShoulderRight = (ShoulderRidingEntity)entity2;
                }
            }

            if (entity2 != null) {
                entity2.xo = entity1.xo;
                entity2.yo = entity1.yo;
                entity2.zo = entity1.zo;
                entity2.setPosRaw(entity1.getX(), entity1.getY(), entity1.getZ());
                entity2.xRotO = entity1.xRotO;
                entity2.yRotO = entity1.yRotO;
                entity2.setXRot(entity1.getXRot());
                entity2.setYRot(entity1.getYRot());
                if (entity2 instanceof LivingEntity && entity1 instanceof LivingEntity) {
                    ((LivingEntity)entity2).yBodyRotO = ((LivingEntity)entity1).yBodyRotO;
                    ((LivingEntity)entity2).yBodyRot = ((LivingEntity)entity1).yBodyRot;
                }
            }
        }

        this.model = ArrayUtils.firstNonNull(CustomStaticModels.getParrotModel(), this.parrotModelOriginal);
        p_431021_.pushPose();
        p_431021_.translate(p_431155_ ? 0.4F : -0.4F, p_425167_.isCrouching ? -1.3F : -1.5F, 0.0F);
        ParrotRenderState parrotrenderstate = new ParrotRenderState();
        parrotrenderstate.pose = ParrotModel.Pose.ON_SHOULDER;
        parrotrenderstate.ageInTicks = p_425167_.ageInTicks;
        parrotrenderstate.walkAnimationPos = p_425167_.walkAnimationPos;
        parrotrenderstate.walkAnimationSpeed = p_425167_.walkAnimationSpeed;
        parrotrenderstate.yRot = p_428398_;
        parrotrenderstate.xRot = p_428084_;
        parrotrenderstate.entity = entity2;
        EntityRenderState entityrenderstate = RenderState.setEntityRenderState(parrotrenderstate);
        p_422981_.submitModel(
            this.model,
            parrotrenderstate,
            p_431021_,
            this.model.renderType(ParrotRenderer.getVariantTexture(p_452298_)),
            p_427224_,
            OverlayTexture.NO_OVERLAY,
            p_425167_.outlineColor,
            null
        );
        RenderState.setEntityRenderState(entityrenderstate);
        p_431021_.popPose();
    }

    private Entity makeEntity(CompoundTag compoundtag, Player player) {
        ValueInput valueinput = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), compoundtag);
        Optional<EntityType<?>> optional = EntityType.by(valueinput);
        if (!optional.isPresent()) {
            return null;
        }

        Entity entity = optional.get().create(player.level(), EntitySpawnReason.JOCKEY);
        if (entity == null) {
            return null;
        }

        entity.load(valueinput);
        SynchedEntityData synchedentitydata = entity.getEntityData();
        if (synchedentitydata != null) {
            synchedentitydata.spawnPosition = player.blockPosition();
            synchedentitydata.spawnBiome = player.level().getBiome(synchedentitydata.spawnPosition).value();
        }

        return entity;
    }
}
