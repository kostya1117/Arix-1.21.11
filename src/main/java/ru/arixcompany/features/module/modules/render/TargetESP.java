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
import ru.arixcompany.ui.clickgui.Colors;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.awt.*;

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
                    .withBlend(BlendFunction.TRANSLUCENT)
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

        matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

        float rotation = (System.currentTimeMillis() % 2000L) / 2000.0f * 360.0f;
        matrices.mulPose(Axis.ZP.rotationDegrees(rotation));

        Color baseColor = Arix.getInstance().getCurrentTheme().getMain();
        Color damageColor = new Color(255, 60, 60);

        float hurtProgress = clamp01((entity.hurtTime - tickDelta) / 10.0f);
        hurtProgress = hurtProgress * hurtProgress;

        int r = lerpInt(baseColor.getRed(), damageColor.getRed(), hurtProgress);
        int g = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(baseColor.getBlue(), damageColor.getBlue(), hurtProgress);

        float baseScale = 1.1f;
        float finalScale = baseScale - (0.18f * hurtProgress);
        matrices.scale(finalScale, finalScale, finalScale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(texLayerND(TEX_CLIENT));

        int alphaValue = (int) (255f * alpha.getOutput());

        vertex.addVertex(matrix, -0.5f, -0.5f, 0)
                .setColor(r, g, b, alphaValue)
                .setUv(0, 1);
        vertex.addVertex(matrix, 0.5f, -0.5f, 0)
                .setColor(r, g, b, alphaValue)
                .setUv(1, 1);
        vertex.addVertex(matrix, 0.5f, 0.5f, 0)
                .setColor(r, g, b, alphaValue)
                .setUv(1, 0);
        vertex.addVertex(matrix, -0.5f, 0.5f, 0)
                .setColor(r, g, b, alphaValue)
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

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private int lerpInt(int from, int to, float delta) {
        return (int) (from + (to - from) * delta);
    }
}