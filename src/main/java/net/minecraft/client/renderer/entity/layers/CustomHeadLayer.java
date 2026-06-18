package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.ChinaHat;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.ColorUtil;

public class CustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> implements IMinecraft {
    private static final float ITEM_SCALE = 0.625F;
    private static final float SKULL_SCALE = 1.1875F;
    private final CustomHeadLayer.Transforms transforms;
    private final Function<SkullBlock.Type, SkullModelBase> skullModels;
    private final PlayerSkinRenderCache playerSkinRenderCache;


    private static final RenderPipeline PIPELINE_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_fill"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)  // было LESS_DEPTH_TEST
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderPipeline PIPELINE_OUTLINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_outline"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)  // было LESS_DEPTH_TEST
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderType RT_FILL = RenderType.create("chinahat_fill",
            RenderSetup.builder(PIPELINE_FILL)
                    .bufferSize(4096)
                    .createRenderSetup());

    private static final RenderType RT_OUTLINE = RenderType.create("chinahat_outline",
            RenderSetup.builder(PIPELINE_OUTLINE)
                    .bufferSize(4096)
                    .createRenderSetup());

    public CustomHeadLayer(RenderLayerParent<S, M> p_234822_, EntityModelSet p_234823_, PlayerSkinRenderCache p_422915_) {
        this(p_234822_, p_234823_, p_422915_, CustomHeadLayer.Transforms.DEFAULT);
    }

    public CustomHeadLayer(RenderLayerParent<S, M> p_234829_, EntityModelSet p_234830_, PlayerSkinRenderCache p_426986_, CustomHeadLayer.Transforms p_377766_) {
        super(p_234829_);
        this.transforms = p_377766_;
        this.skullModels = Util.memoize(p_448342_ -> SkullBlockRenderer.createModel(p_234830_, p_448342_));
        this.playerSkinRenderCache = p_426986_;
    }

    public void submit(PoseStack p_430594_, SubmitNodeCollector p_424787_, int p_428319_, S p_423337_, float p_425556_, float p_428582_) {
        if (!p_423337_.headItem.isEmpty() || p_423337_.wornHeadType != null) {
            p_430594_.pushPose();
            p_430594_.scale(this.transforms.horizontalScale(), 1.0F, this.transforms.horizontalScale());
            M m = this.getParentModel();
            m.root().translateAndRotate(p_430594_);
            m.translateToHead(p_430594_);
            if (p_423337_.wornHeadType != null) {
                p_430594_.translate(0.0F, this.transforms.skullYOffset(), 0.0F);
                p_430594_.scale(1.1875F, -1.1875F, -1.1875F);
                p_430594_.translate(-0.5, 0.0, -0.5);
                SkullBlock.Type skullblock$type = p_423337_.wornHeadType;
                SkullModelBase skullmodelbase = this.skullModels.apply(skullblock$type);
                RenderType rendertype = this.resolveSkullRenderType(p_423337_, skullblock$type);
                SkullBlockRenderer.submitSkull(
                        null, 180.0F, p_423337_.wornHeadAnimationPos, p_430594_, p_424787_, p_428319_, skullmodelbase, rendertype, p_423337_.outlineColor, null
                );
            } else {
                translateToHead(p_430594_, this.transforms);
                p_423337_.headItem.submit(p_430594_, p_424787_, p_428319_, OverlayTexture.NO_OVERLAY, p_423337_.outlineColor);
            }

            p_430594_.popPose();
        }
        //this.renderChinaHat(p_430594_, p_424787_, p_423337_);
    }
    private void renderChinaHat(PoseStack poseStack, SubmitNodeCollector collector, S someState) {
        if (!(someState instanceof AvatarRenderState avatarState)
                || !Arix.getInstance().getModuleRepo().getModule(ChinaHat.class).isState()) {
            return;
        }

        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(avatarState.id);
        if (entity == null) return;

        boolean isSelf = entity == mc.player;
        boolean isFriend = FriendRepo.isFriend(entity.getName().getString());
        if (!isSelf && !isFriend) return;

        float radius = Math.max(avatarState.boundingBoxWidth, 0.6f);
        final int segments = 365;
        final float hatHeight = 0.3f;
        final float alpha = 0.8F;
        int multiplier = 2;
        int centerColor = ColorUtil.applyAlpha(ColorUtil.jumpBlend(0, 1.0f), alpha);

        poseStack.pushPose();
        this.getParentModel().root().translateAndRotate(poseStack);
        this.getParentModel().translateToHead(poseStack);

        boolean hasHelmet = !avatarState.headItem.isEmpty() || !avatarState.heldOnHead.isEmpty();
        if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            hasHelmet = hasHelmet || !livingEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
        }

        float offset = hasHelmet ? 0.52F : 0.44F;
        poseStack.translate(0.0F, -offset, 0.0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));


        collector.order(Integer.MAX_VALUE - 1).submitCustomGeometry(poseStack, RT_FILL, (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                int color1 = ColorUtil.fade(i * multiplier);
                int color2 = ColorUtil.fade((i + 1) * multiplier);

                float angle1 = (float) (i * (Math.PI * 2.0) / segments);
                float angle2 = (float) ((i + 1) * (Math.PI * 2.0) / segments);

                float x1 = -Mth.sin(angle1) * radius;
                float z1 = Mth.cos(angle1) * radius;
                float x2 = -Mth.sin(angle2) * radius;
                float z2 = Mth.cos(angle2) * radius;

                buffer.addVertex(pose.pose(), 0.0F, hatHeight, 0.0F).setColor(centerColor);
                buffer.addVertex(pose.pose(), 0.0F, hatHeight, 0.0F).setColor(centerColor);
                buffer.addVertex(pose.pose(), x2, 0.0F, z2).setColor(ColorUtil.applyAlpha(color2, alpha));
                buffer.addVertex(pose.pose(), x1, 0.0F, z1).setColor(ColorUtil.applyAlpha(color1, alpha));
            }
        });

        collector.order(Integer.MAX_VALUE - 1).submitCustomGeometry(poseStack, RT_OUTLINE, (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                int color1 = ColorUtil.fade(i * multiplier);
                int color2 = ColorUtil.fade((i + 1) * multiplier);

                float angle1 = (float) (i * (Math.PI * 2.0) / segments);
                float angle2 = (float) ((i + 1) * (Math.PI * 2.0) / segments);

                float x1 = -Mth.sin(angle1) * radius;
                float z1 = Mth.cos(angle1) * radius;
                float x2 = -Mth.sin(angle2) * radius;
                float z2 = Mth.cos(angle2) * radius;

                buffer.addVertex(pose.pose(), x1, 0.0F, z1)
                        .setColor(ColorUtil.applyAlpha(color1, 1.0F))
                        .setNormal(pose, 0.0F, 1.0F, 0.0F)
                        .setLineWidth(4);

                buffer.addVertex(pose.pose(), x2, 0.0F, z2)
                        .setColor(ColorUtil.applyAlpha(color2, 1.0F))
                        .setNormal(pose, 0.0F, 1.0F, 0.0F)
                        .setLineWidth(4);
            }
        });

        poseStack.popPose();
    }

    private RenderType resolveSkullRenderType(LivingEntityRenderState p_431568_, SkullBlock.Type p_426369_) {
        if (p_426369_ == SkullBlock.Types.PLAYER) {
            ResolvableProfile resolvableprofile = p_431568_.wornHeadProfile;
            if (resolvableprofile != null) {
                return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
            }
        }

        return SkullBlockRenderer.getSkullRenderType(p_426369_, null);
    }

    public static void translateToHead(PoseStack p_174484_, CustomHeadLayer.Transforms p_366424_) {
        p_174484_.translate(0.0F, -0.25F + p_366424_.yOffset(), 0.0F);
        p_174484_.mulPose(Axis.YP.rotationDegrees(180.0F));
        p_174484_.scale(0.625F, -0.625F, -0.625F);
    }

    public record Transforms(float yOffset, float skullYOffset, float horizontalScale) {
        public static final CustomHeadLayer.Transforms DEFAULT = new CustomHeadLayer.Transforms(0.0F, 0.0F, 1.0F);
    }
}