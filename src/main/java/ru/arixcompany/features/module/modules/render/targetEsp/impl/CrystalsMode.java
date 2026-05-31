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
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseOutCubic;

import java.awt.*;
import java.util.List;

@Getter
public class CrystalsMode extends TargetEspMode {
    private final ValueSetting crystalSize = new ValueSetting("Размер кристаллов")
            .range(0.1f, 2.0f)
            .setValue(0.8f)
            .setStep(0.1f);

    private final ValueSetting crystalCount = new ValueSetting("Кол-во кристаллов")
            .range(8, 30)
            .setValue(20)
            .setStep(1);

    private final Animation animation = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);
    private LivingEntity lastTarget;

    private static final RenderPipeline PIPELINE_ADDITIVE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/textured_quads_additive"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderPipeline PIPELINE_CRYSTAL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/crystal_triangles"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    public CrystalsMode() {
        super("Кристаллы");
    }

    @Override
    public List<Setting> getSettings() {
        return List.of(crystalSize, crystalCount);
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

        float easedAnim = (float) easeOutCubic(animation.getOutput());

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Vec3 targetPos = new Vec3(
                lerp(lastTarget.getX(), lastTarget.xOld, tickDelta),
                lerp(lastTarget.getY(), lastTarget.yOld, tickDelta),
                lerp(lastTarget.getZ(), lastTarget.zOld, tickDelta)
        );

        float time = (mc.player.tickCount + tickDelta) * 3.2f;
        float entityHeight = lastTarget.getBbHeight();
        float entityWidth = lastTarget.getBbWidth();
        float halfWidth = entityWidth * 0.5f;

        Color baseColor = Arix.getInstance().getCurrentTheme().getMain();
        float rawHurt = clamp01((lastTarget.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);
        Color damageColor = new Color(255, 60, 60);

        int cr = lerpInt(baseColor.getRed(), damageColor.getRed(), hurtProgress);
        int cg = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int cb = lerpInt(baseColor.getBlue(), damageColor.getBlue(), hurtProgress);

        int count = crystalCount.getInt();
        float scaleSetting = crystalSize.getValue();

        PoseStack matrices = event.getMatrixStack();
        matrices.pushPose();
        matrices.translate(
                targetPos.x - cam.x,
                targetPos.y - cam.y,
                targetPos.z - cam.z
        );

        try (ByteBufferBuilder glowAllocator = new ByteBufferBuilder(4096)) {
            MultiBufferSource.BufferSource glowBuffer = MultiBufferSource.immediate(glowAllocator);

            for (int i = 0; i < count; i++) {
                float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
                float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
                float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;

                float angleOffset = i * (360f / count) + seed1 * 12f;
                float angle = time + angleOffset;
                float radius = halfWidth + 0.25f + seed3 * 0.15f;

                float x = radius * (float) Math.cos(Math.toRadians(angle));
                float z = radius * (float) Math.sin(Math.toRadians(angle));
                float y = seed2 * entityHeight * 1.05f;

                float glowSize = 0.15f * easedAnim * scaleSetting * 3.2f;
                int glowAlpha = (int) (255 * easedAnim * 0.25f);

                matrices.pushPose();
                matrices.translate(x, y, z);
                matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

                Matrix4f matrix = matrices.last().pose();
                VertexConsumer vertex = glowBuffer.getBuffer(createTexturedRenderType(Textures.glow));

                float hs = glowSize / 2f;
                vertex.addVertex(matrix, -hs, -hs, 0).setUv(0, 1).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, hs, -hs, 0).setUv(1, 1).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, hs, hs, 0).setUv(1, 0).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, -hs, hs, 0).setUv(0, 0).setColor(cr, cg, cb, glowAlpha);

                matrices.popPose();
            }

            glowBuffer.endBatch();
        }

        try (ByteBufferBuilder crystalAllocator = new ByteBufferBuilder(8192)) {
            MultiBufferSource.BufferSource crystalBuffer = MultiBufferSource.immediate(crystalAllocator);

            for (int i = 0; i < count; i++) {
                float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
                float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
                float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;

                float angleOffset = i * (360f / count) + seed1 * 12f;
                float angle = time + angleOffset;
                float radius = halfWidth + 0.25f + seed3 * 0.15f;

                float x = radius * (float) Math.cos(Math.toRadians(angle));
                float z = radius * (float) Math.sin(Math.toRadians(angle));
                float y = seed2 * entityHeight * 1.05f;

                float crystalScale = 0.15f * easedAnim * scaleSetting;
                int crystalAlpha = (int) (180 * easedAnim * 0.8f);

                drawCrystal(matrices, crystalBuffer, x, y, z, crystalScale, angle, cr, cg, cb, crystalAlpha);
            }

            crystalBuffer.endBatch();
        }

        matrices.popPose();
    }

    private void drawCrystal(PoseStack matrices, MultiBufferSource.BufferSource buffer,
                             float x, float y, float z, float scale,
                             float yaw, int r, int g, int b, int a) {
        matrices.pushPose();
        matrices.translate(x, y, z);
        matrices.mulPose(Axis.YP.rotationDegrees(-yaw + 90f));
        matrices.scale(scale, scale, scale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(createCrystalRenderType());

        int rL = Math.min(255, (int) (r * 1.3f));
        int gL = Math.min(255, (int) (g * 1.3f));
        int bL = Math.min(255, (int) (b * 1.3f));

        int rD = (int) (r * 0.6f);
        int gD = (int) (g * 0.6f);
        int bD = (int) (b * 0.6f);

        float w = 0.5f;
        float h = 1.0f;

        addTriangle(vertex, matrix, 0, 0, h, -w, 0, 0, 0, w, 0, rL, gL, bL, a);
        addTriangle(vertex, matrix, 0, 0, h, 0, w, 0, w, 0, 0, rL, gL, bL, a);
        addTriangle(vertex, matrix, 0, 0, h, w, 0, 0, 0, -w, 0, r, g, b, a);
        addTriangle(vertex, matrix, 0, 0, h, 0, -w, 0, -w, 0, 0, r, g, b, a);

        addTriangle(vertex, matrix, 0, 0, -h, 0, w, 0, -w, 0, 0, rD, gD, bD, a);
        addTriangle(vertex, matrix, 0, 0, -h, w, 0, 0, 0, w, 0, rD, gD, bD, a);
        addTriangle(vertex, matrix, 0, 0, -h, 0, -w, 0, w, 0, 0, rD, gD, bD, a);
        addTriangle(vertex, matrix, 0, 0, -h, -w, 0, 0, 0, -w, 0, rD, gD, bD, a);

        matrices.popPose();
    }

    private void addTriangle(VertexConsumer vertex, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int b, int a) {
        vertex.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        vertex.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        vertex.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
    }

    private RenderType createTexturedRenderType(Identifier texture) {
        return RenderType.create(
                texture + "_crystals",
                RenderSetup.builder(PIPELINE_ADDITIVE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, texture)
                        .createRenderSetup()
        );
    }

    private RenderType createCrystalRenderType() {
        return RenderType.create(
                "arix_crystal_triangles",
                RenderSetup.builder(PIPELINE_CRYSTAL)
                        .bufferSize(8192)
                        .createRenderSetup()
        );
    }

    private static double easeOutCubic(double x) {
        return 1.0 - Math.pow(1.0 - x, 3.0);
    }
}
