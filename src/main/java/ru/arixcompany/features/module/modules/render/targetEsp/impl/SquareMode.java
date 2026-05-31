package ru.arixcompany.features.module.modules.render.targetEsp.impl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.modules.render.targetEsp.TargetEspMode;
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.awt.*;

@Getter
public class SquareMode extends TargetEspMode {
    private final Animation alpha = new EaseInOutQuad(350, 1.0);
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

    public SquareMode() {
        super("Квадрат");
    }

    @Override
    public void render(EventRender3D event, LivingEntity target, float tickDelta) {
        alpha.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (alpha.getOutput() <= 0.01f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        try (ByteBufferBuilder allocator = new ByteBufferBuilder(1024)) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            renderSquare(event.getMatrixStack(), buffer, lastTarget, tickDelta);
            buffer.endBatch();
        }
    }

    private void renderSquare(PoseStack matrices, MultiBufferSource.BufferSource buffer,
                              LivingEntity entity, float tickDelta) {
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

        float rawProgress = clamp01((entity.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawProgress * Math.PI);

        int r = lerpInt(baseColor.getRed(), damageColor.getRed(), hurtProgress);
        int g = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(baseColor.getBlue(), damageColor.getBlue(), hurtProgress);

        float baseScale = 1.1f;
        float finalScale = baseScale + (0.12f * hurtProgress);
        matrices.scale(finalScale, finalScale, finalScale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(createRenderType(Textures.target));

        int alphaValue = (int) (255f * alpha.getOutput());

        vertex.addVertex(matrix, -0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 1);
        vertex.addVertex(matrix, 0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 1);
        vertex.addVertex(matrix, 0.5f, 0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 0);
        vertex.addVertex(matrix, -0.5f, 0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 0);

        matrices.popPose();
    }

    private RenderType createRenderType(Identifier texture) {
        return RenderType.create(
                texture + "_square",
                RenderSetup.builder(PIPELINE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, texture)
                        .createRenderSetup()
        );
    }
}
