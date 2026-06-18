package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventHeadLayerRender;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.ColorUtil;

public class ChinaHat extends Module {
    private static final RenderPipeline PIPELINE_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_fill"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)  // Меняем на GREATER
                    .withDepthWrite(true)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderPipeline PIPELINE_OUTLINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_outline"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)  // Меняем на GREATER
                    .withDepthWrite(true)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderType RT_FILL = RenderType.create(
            "chinahat_fill",
            RenderSetup.builder(PIPELINE_FILL)
                    .bufferSize(4096)
                    .createRenderSetup()
    );

    private static final RenderType RT_OUTLINE = RenderType.create(
            "chinahat_outline",
            RenderSetup.builder(PIPELINE_OUTLINE)
                    .bufferSize(4096)
                    .createRenderSetup()
    );

    private final ListSetting targets = new ListSetting("Показывать")
            .value("Себе", "Друзьям", "Остальным")
            .selected("Себе", "Друзьям");

    public ChinaHat() {
        super("ChinaHat", Category.Render);
        setup(targets);
    }

    @EventHandler
    public void onRender(EventHeadLayerRender event){
        if (!(event.getState() instanceof AvatarRenderState avatarState)
                || !Arix.getInstance().getModuleRepo().getModule(ChinaHat.class).isState()) {
            return;
        }
        PoseStack poseStack = event.getMatrix();
        SubmitNodeCollector collector = event.getCollector();
        EntityModel<?> model = event.getModel();

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
        model.root().translateAndRotate(poseStack);
        if (model instanceof HeadedModel headedModel)
            headedModel.translateToHead(poseStack);

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
}