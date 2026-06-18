package ru.arixcompany.features.module.modules.render.targetEsp.impl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
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
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.cubic.EaseOutCubic;

import java.awt.*;

@Getter
public class CircleMode extends TargetEspMode {
    private final Animation animation = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);
    private LivingEntity lastTarget;

    private float prevCircleStep = 0f;
    private float circleStep = 0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/triangle_strip"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public CircleMode() {
        super("Круг");
    }

    @Override
    public void onUpdate() {
        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    @Override
    public void render(EventRender3D event, LivingEntity target, float tickDelta) {
        animation.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (animation.getOutput() <= 0.0f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        double cs = prevCircleStep + (circleStep - prevCircleStep) * tickDelta;
        double prevSinAnim = absSinAnimation(cs - 0.45f);
        double sinAnim = absSinAnimation(cs);

        double ix = lerp(lastTarget.getX(), lastTarget.xOld, tickDelta) - cam.x;
        double iy = lerp(lastTarget.getY(), lastTarget.yOld, tickDelta) - cam.y;
        double iz = lerp(lastTarget.getZ(), lastTarget.zOld, tickDelta) - cam.z;

        double bottomY = iy + prevSinAnim * lastTarget.getBbHeight();
        double topY = iy + sinAnim * lastTarget.getBbHeight();

        Color themeColor = Arix.getInstance().getCurrentTheme().getMain();

        float rawHurt = clamp01((lastTarget.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);
        Color damageColor = new Color(255, 60, 60);
        int cr = lerpInt(themeColor.getRed(), damageColor.getRed(), hurtProgress);
        int cg = lerpInt(themeColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int cb = lerpInt(themeColor.getBlue(), damageColor.getBlue(), hurtProgress);

        float animVal = (float) animation.getOutput();
        float width = lastTarget.getBbWidth() * 0.8f;

        int segments = 64;

        PoseStack matrices = event.getMatrixStack();
        matrices.pushPose();

        try (ByteBufferBuilder allocator = new ByteBufferBuilder(segments * 2 * 32 + 512)) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);
            Matrix4f matrix = matrices.last().pose();

            VertexConsumer vertex = bufferSource.getBuffer(createRenderType());
            for (int i = 0; i <= segments; i++) {
                double angle = i * (Math.PI * 2.0) / segments;
                float px = (float) (ix + Math.cos(angle) * width);
                float pz = (float) (iz + Math.sin(angle) * width);

                int alphaTop = (int) (170 * animVal);

                vertex.addVertex(matrix, px, (float) topY, pz)
                        .setColor(cr, cg, cb, alphaTop);
                vertex.addVertex(matrix, px, (float) bottomY, pz)
                        .setColor(cr, cg, cb, 0);
            }

            bufferSource.endBatch();
        }

        try (ByteBufferBuilder allocator2 = new ByteBufferBuilder(segments * 2 * 32 + 512)) {
            MultiBufferSource.BufferSource bufferSource2 = MultiBufferSource.immediate(allocator2);
            Matrix4f matrix = matrices.last().pose();

            VertexConsumer vertex2 = bufferSource2.getBuffer(createRenderType());
            for (int i = segments; i >= 0; i--) {
                double angle = i * (Math.PI * 2.0) / segments;
                float px = (float) (ix + Math.cos(angle) * width);
                float pz = (float) (iz + Math.sin(angle) * width);

                int alphaTop = (int) (170 * animVal);

                vertex2.addVertex(matrix, px, (float) topY, pz)
                        .setColor(cr, cg, cb, alphaTop);
                vertex2.addVertex(matrix, px, (float) bottomY, pz)
                        .setColor(cr, cg, cb, 0);
            }

            bufferSource2.endBatch();
        }

        matrices.popPose();
    }

    private RenderType createRenderType() {
        return RenderType.create(
                "arix_circle_target_esp",
                RenderSetup.builder(PIPELINE)
                        .bufferSize(4096)
                        .createRenderSetup()
        );
    }

    private static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }
}
