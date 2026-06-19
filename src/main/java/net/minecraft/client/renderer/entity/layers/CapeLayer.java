package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.optifine.util.PlayerUtils;
import ru.arixcompany.features.module.modules.render.Cape;
import ru.arixcompany.features.module.modules.render.cape.CapeMovement;
import ru.arixcompany.features.module.modules.render.cape.WindMode;
import ru.arixcompany.features.module.modules.render.cape.sim.BasicSimulation;
import ru.arixcompany.features.module.modules.render.cape.util.Vector3;
import ru.arixcompany.utils.Textures;

public class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HumanoidModel<AvatarRenderState> model;
    private final EquipmentAssetManager equipmentAssets;
    private ModelPart[] customCape;

    public CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_116602_, EntityModelSet p_364158_, EquipmentAssetManager p_378632_) {
        super(p_116602_);
        this.model = new PlayerCapeModel(p_364158_.bakeLayer(ModelLayers.PLAYER_CAPE));
        this.equipmentAssets = p_378632_;
    }

    private boolean hasLayer(ItemStack p_362441_, EquipmentClientInfo.LayerType p_377432_) {
        Equippable equippable = p_362441_.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.assetId().isEmpty()) {
            EquipmentClientInfo equipmentclientinfo = this.equipmentAssets.get(equippable.assetId().get());
            return !equipmentclientinfo.getLayers(p_377432_).isEmpty();
        } else {
            return false;
        }
    }

    public void submit(PoseStack p_431599_, SubmitNodeCollector p_430860_, int p_427257_, AvatarRenderState p_428454_, float p_429917_, float p_424453_) {
        if (p_428454_.isInvisible || !p_428454_.showCape) return;

        PlayerSkin playerskin = p_428454_.skin;
        AbstractClientPlayer abstractclientplayer = p_428454_.entity instanceof AbstractClientPlayer ? (AbstractClientPlayer)p_428454_.entity : null;

        if (Cape.isEnabled()) {
            if (Cape.shouldRender(p_428454_.entity)) {
                renderWaveyCape(p_431599_, p_430860_, p_427257_, p_428454_, abstractclientplayer);
            }
            return;
        }

        Identifier identifier = abstractclientplayer != null ? abstractclientplayer.getLocationCape() : PlayerUtils.getTexturePath(playerskin.cape());
        if (identifier != null && !this.hasLayer(p_428454_.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
            p_431599_.pushPose();
            if (this.hasLayer(p_428454_.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
                p_431599_.translate(0.0F, -0.053125F, 0.06875F);
            }
            p_430860_.submitModel(
                this.model, p_428454_, p_431599_, RenderTypes.entitySolid(identifier), p_427257_, OverlayTexture.NO_OVERLAY, p_428454_.outlineColor, null
            );
            p_431599_.popPose();
        }
    }

    private void renderWaveyCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, AbstractClientPlayer player) {
        if (this.hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS) || player == null) return;

        if (customCape == null || customCape.length != Cape.CAPE_PART_COUNT) {
            buildMesh();
        }

        boolean useSimulation = Cape.getMovement() != CapeMovement.VANILLA && player.capeSimulation != null && !player.capeSimulation.empty();
        Identifier capeTexture = Textures.cape;

        for (int part = 0; part < Cape.CAPE_PART_COUNT; part++) {
            ModelPart modelPart = customCape[part];
            if (useSimulation) {
                modifyPoseStackSimulation(poseStack, player.capeSimulation, player, state, part);
            } else {
                modifyPoseStackVanilla(poseStack, player, state, part);
            }
            collector.submitModelPart(modelPart, poseStack, RenderTypes.entityCutout(capeTexture), light, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }
    }

    private void buildMesh() {
        customCape = new ModelPart[Cape.CAPE_PART_COUNT];
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        for (int i = 0; i < Cape.CAPE_PART_COUNT; i++) {
            partDefinition.addOrReplaceChild("customCape_" + i,
                CubeListBuilder.create().texOffs(0, (int)(i * (16f / Cape.CAPE_PART_COUNT)))
                    .addBox(-5.0F, i * (16f / Cape.CAPE_PART_COUNT), -1.0F,
                        10.0F, (16f / Cape.CAPE_PART_COUNT), 1.0F,
                        CubeDeformation.NONE, 1.0F, 0.5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        }
        ModelPart baked = partDefinition.bake(64, 64);
        for (int i = 0; i < Cape.CAPE_PART_COUNT; i++) {
            customCape[i] = baked.getChild("customCape_" + i);
        }
    }

    private void modifyPoseStackVanilla(PoseStack poseStack, AbstractClientPlayer player, AvatarRenderState state, int part) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.125D);

        double d = Mth.lerp(1f, player.avatarState().getInterpolatedCloakX(1f), player.avatarState().getInterpolatedCloakX(1f));
        double e = Mth.lerp(1f, player.avatarState().getInterpolatedCloakY(1f), player.avatarState().getInterpolatedCloakY(1f));
        double m = Mth.lerp(1f, player.avatarState().getInterpolatedCloakZ(1f), player.avatarState().getInterpolatedCloakZ(1f));

        d -= Mth.lerp(1f, player.xo, player.getX());
        e -= Mth.lerp(1f, player.yo, player.getY());
        m -= Mth.lerp(1f, player.zo, player.getZ());

        float n = player.yBodyRotO + player.yBodyRot - player.yBodyRotO;
        double o = Mth.sin(n * 0.017453292F);
        double p = -Mth.cos(n * 0.017453292F);

        float height = (float) e * 10.0F;
        height = Mth.clamp(height, -6.0F, 32.0F);
        float swing = (float) (d * o + m * p) * easeOutSine(1.0F / Cape.CAPE_PART_COUNT * part) * 100;
        swing = Mth.clamp(swing, 0.0F, 150.0F * easeOutSine(1F / Cape.CAPE_PART_COUNT * part));
        float sidewaysRotationOffset = (float) (d * p - m * o) * 100.0F;
        sidewaysRotationOffset = Mth.clamp(sidewaysRotationOffset, -20.0F, 20.0F);

        float t = Mth.lerp(1f, player.avatarState().getInterpolatedBob(1f), player.avatarState().getInterpolatedBob(1f));
        height += Mth.sin(Mth.lerp(1f, player.avatarState().getInterpolatedWalkDistance(1f), player.avatarState().getInterpolatedWalkDistance(1f)) * 6.0F) * 32.0F * t;

        if (player.isCrouching()) {
            height += 25.0F;
            poseStack.translate(0, 0.15F, 0);
        }

        float naturalWindSwing = getNatrualWindSwing(part);

        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + swing / 2.0F + height + naturalWindSwing));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sidewaysRotationOffset / 2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - sidewaysRotationOffset / 2.0F));
    }

    private void modifyPoseStackSimulation(PoseStack poseStack, BasicSimulation simulation, AbstractClientPlayer player, AvatarRenderState state, int part) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.125D);

        float delta = 1f;
        float x = simulation.getPoints().get(part).getLerpX(delta) - simulation.getPoints().get(0).getLerpX(delta);
        if (x > 0) x = 0;
        float y = simulation.getPoints().get(0).getLerpY(delta) - part - simulation.getPoints().get(part).getLerpY(delta);
        float z = simulation.getPoints().get(0).getLerpZ(delta) - simulation.getPoints().get(part).getLerpZ(delta);

        float sidewaysRotationOffset = 0;
        float partRotation = getRotation(delta, part, simulation);

        float height = 0;
        if (player.isCrouching()) {
            height += 25.0F;
            poseStack.translate(0, 0.15F, 0);
        }

        float naturalWindSwing = getNatrualWindSwing(part);

        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + height + naturalWindSwing));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sidewaysRotationOffset / 2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - sidewaysRotationOffset / 2.0F));
        poseStack.translate(-z / Cape.CAPE_PART_COUNT, y / Cape.CAPE_PART_COUNT, x / Cape.CAPE_PART_COUNT);
        poseStack.translate(0, 0.48F / 16, -0.48F / 16);
        poseStack.translate(0, part * 1f / Cape.CAPE_PART_COUNT, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-partRotation));
        poseStack.translate(0, -part * 1f / Cape.CAPE_PART_COUNT, 0);
        poseStack.translate(0, -0.48F / 16, 0.48F / 16);
    }

    private float getRotation(float delta, int part, BasicSimulation simulation) {
        if (part == Cape.CAPE_PART_COUNT - 1) return getRotation(delta, part - 1, simulation);
        float angle = (float) getAngle(
            simulation.getPoints().get(part).getLerpedPos(delta),
            simulation.getPoints().get(part + 1).getLerpedPos(delta));
        return angle;
    }

    private double getAngle(Vector3 a, Vector3 b) {
        Vector3 angle = b.subtract(a);
        return Math.toDegrees(Math.atan2(angle.x, angle.y)) + 180;
    }

    private float getNatrualWindSwing(int part) {
        if (Cape.getWindMode() != WindMode.WAVES) return 0;
        long highlightedPart = (System.currentTimeMillis() / 3) % 360;
        float relativePart = (float) (part + 1) / Cape.CAPE_PART_COUNT;
        return (float) (Math.sin(Math.toRadians((relativePart) * 360 - highlightedPart)) * 3);
    }

    private static float easeOutSine(float x) {
        return Mth.sin((x * Mth.PI) / 2f);
    }
}
