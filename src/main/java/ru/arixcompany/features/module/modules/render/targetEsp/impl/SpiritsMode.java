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
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseOutCubic;

import java.awt.*;

@Getter
public class SpiritsMode extends TargetEspMode {
    private final Animation animation = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);
    private LivingEntity lastTarget;
    private float animationProgress = 0.0f;
    private long currentTime = System.currentTimeMillis();

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/textured_quads_additive"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public SpiritsMode() {
        super("Призраки");
    }

    @Override
    public void render(EventRender3D event, LivingEntity target, float tickDelta) {
        animation.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (animation.getOutput() <= 0.0f) {
            lastTarget = null;
            return;
        }

        if (target != null) {
            if (lastTarget == null) {
                currentTime = System.currentTimeMillis();
            }
            lastTarget = target;
        }

        if (lastTarget == null) return;

        long now = System.currentTimeMillis();
        animationProgress += (4L * (now - currentTime)) / 600.0f;
        currentTime = now;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        double x = lerp(lastTarget.getX(), lastTarget.xOld, tickDelta) - cam.x;
        double y = lerp(lastTarget.getY(), lastTarget.yOld, tickDelta) - cam.y;
        double z = lerp(lastTarget.getZ(), lastTarget.zOld, tickDelta) - cam.z;

        Color color = Arix.getInstance().getCurrentTheme().getMain();
        float animVal = (float) animation.getOutput();
        int alphaValue = (int) (animVal * 255f);

        float rawHurt = clamp01((lastTarget.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);

        Color damageColor = new Color(255, 60, 60);
        int r = lerpInt(color.getRed(), damageColor.getRed(), hurtProgress);
        int g = lerpInt(color.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(color.getBlue(), damageColor.getBlue(), hurtProgress);

        int layers = 3;
        int particles = 12;

        try (ByteBufferBuilder allocator = new ByteBufferBuilder(4096)) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);
            PoseStack matrices = event.getMatrixStack();
            matrices.pushPose();

            for (int i = 0; i < layers * layers; i += layers) {
                for (int j = 0; j < particles; j++) {
                    float f2 = animationProgress + j * 0.1f;
                    float radius = 0.8f;
                    float heightOffset = 0.5f;
                    int n5 = (int) Math.pow(i, 2.0);

                    float px = radius * (float) Math.sin(f2 + n5);
                    float py = (float) (heightOffset + 0.3f * Math.sin(animationProgress + j * 0.2f) + 0.2f * i);
                    float pz = radius * (float) Math.cos(f2 - n5);

                    float particleScale = animVal * (0.006f + j / 2000.0f);

                    matrices.pushPose();
                    matrices.translate(x + px, y + py, z + pz);
                    matrices.scale(particleScale, particleScale, particleScale);
                    matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

                    Matrix4f matrix = matrices.last().pose();
                    VertexConsumer vertex = bufferSource.getBuffer(createRenderType(Textures.glow));

                    vertex.addVertex(matrix, -19, 19, 0).setUv(0, 1).setColor(r, g, b, alphaValue);
                    vertex.addVertex(matrix, 19, 19, 0).setUv(1, 1).setColor(r, g, b, alphaValue);
                    vertex.addVertex(matrix, 19, -19, 0).setUv(1, 0).setColor(r, g, b, alphaValue);
                    vertex.addVertex(matrix, -19, -19, 0).setUv(0, 0).setColor(r, g, b, alphaValue);

                    matrices.popPose();
                }
            }

            bufferSource.endBatch();
            matrices.popPose();
        }
    }

    private RenderType createRenderType(Identifier texture) {
        return RenderType.create(
                texture + "_spirits",
                RenderSetup.builder(PIPELINE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, texture)
                        .createRenderSetup()
        );
    }
}
