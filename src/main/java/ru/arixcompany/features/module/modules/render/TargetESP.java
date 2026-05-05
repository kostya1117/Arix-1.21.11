package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.KillAura;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

public class TargetESP extends Module {

    private static final Identifier TEX_CLIENT =
            Identifier.withDefaultNamespace("textures/arix/target.png");

    private static final Animation alpha = new EaseInOutQuad(350, 1.0);

    private LivingEntity lastTarget;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/textured_quads"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public TargetESP() {
        super("TargetESP", Category.Render);
    }

    @EventHandler
    public void onRender(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        KillAura hitKillAura = (KillAura) Arix.getInstance()
                .getModuleRepo().getModule(KillAura.class);

        if (hitKillAura == null) return;

        LivingEntity target = KillAura.getTarget();

        alpha.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (alpha.getOutput() <= 0.01f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        ByteBufferBuilder allocator = new ByteBufferBuilder(1024);
        MultiBufferSource.BufferSource buffer =
                MultiBufferSource.immediate(allocator);

        try {
            renderImage(e.getMatrixStack(), buffer, lastTarget, e.getTickDelta());
            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    private void renderImage(PoseStack matrices,
                             MultiBufferSource.BufferSource buffer,
                             LivingEntity entity,
                             float tickDelta) {

        Vec3 pos = entity.getPosition(tickDelta);
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        matrices.pushPose();
        matrices.translate(
                pos.x - cam.x,
                pos.y - cam.y + entity.getBbHeight() / 1.75,
                pos.z - cam.z
        );

        matrices.mulPose(
                Axis.YP.rotationDegrees(
                        -mc.gameRenderer.getMainCamera().xRot()
                )
        );
        matrices.mulPose(
                Axis.XP.rotationDegrees(
                        mc.gameRenderer.getMainCamera().yRot()
                )
        );

        float rotation = (float)
                ((Math.sin(System.currentTimeMillis() / 1600.0) + 1) / 2 * 720);

        matrices.mulPose(
                Axis.ZP.rotationDegrees(rotation)
        );

        float scale = 1.5f;
        matrices.scale(scale, scale, scale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex =
                buffer.getBuffer(texLayerND(TEX_CLIENT));

        int alphaValue = (int) (255f * alpha.getOutput());

        vertex.addVertex(matrix, -0.5f, -0.5f, 0)
                .setColor(255, 255, 255, alphaValue)
                .setUv(0, 1);
        vertex.addVertex(matrix, 0.5f, -0.5f, 0)
                .setColor(255, 255, 255, alphaValue)
                .setUv(1, 1);
        vertex.addVertex(matrix, 0.5f, 0.5f, 0)
                .setColor(255, 255, 255, alphaValue)
                .setUv(1, 0);
        vertex.addVertex(matrix, -0.5f, 0.5f, 0)
                .setColor(255, 255, 255, alphaValue)
                .setUv(0, 0);

        matrices.popPose();
    }

    private RenderType texLayerND(Identifier tex) {
        return RenderType.create(
                tex.toString(),
                RenderSetup.builder(PIPELINE)
                        .bufferSize(1024)
                        .withTexture(RenderType.SAMPLER0, tex)
                        .createRenderSetup()
        );
    }
}