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
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final float[][] CRYSTAL_RINGS = {
            { 0.60f, 0.2f,  0.7f, 4, 0.0f },
            { 0.70f, 0.8f, -0.5f, 3, 1.0f },
            { 0.65f, 1.4f,  0.6f, 4, 0.5f },
            { 0.55f, 2.0f, -0.4f, 3, 1.8f }
    };

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

        float totalTime = (mc.player.tickCount + tickDelta);
        float time = totalTime * 3.2f;
        float entityHeight = lastTarget.getBbHeight();
        float entityWidth = lastTarget.getBbWidth();

        Color themeColor = Arix.getInstance().getCurrentTheme().getMain();
        float rawHurt = clamp01((lastTarget.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);
        Color damageColor = new Color(255, 60, 60);

        int cr = lerpInt(themeColor.getRed(), damageColor.getRed(), hurtProgress);
        int cg = lerpInt(themeColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int cb = lerpInt(themeColor.getBlue(), damageColor.getBlue(), hurtProgress);

        float scaleSetting = crystalSize.getValue();
        float crystalAlphaValue = easedAnim;

        PoseStack matrices = event.getMatrixStack();
        matrices.pushPose();
        matrices.translate(targetPos.x - cam.x, targetPos.y - cam.y, targetPos.z - cam.z);

        try (ByteBufferBuilder glowAllocator = new ByteBufferBuilder(4096);
             ByteBufferBuilder crystalAllocator = new ByteBufferBuilder(8192)) {
            MultiBufferSource.BufferSource glowBuffer = MultiBufferSource.immediate(glowAllocator);
            MultiBufferSource.BufferSource crystalBuffer = MultiBufferSource.immediate(crystalAllocator);

            for (int ring = 0; ring < CRYSTAL_RINGS.length; ring++) {
                float orbitRadius = CRYSTAL_RINGS[ring][0] * (entityWidth + 0.4f); // адаптируем под хитбокс
                float ringY       = CRYSTAL_RINGS[ring][1] * (entityHeight / 2.0f); // адаптируем высоту
                float orbitSpeed  = CRYSTAL_RINGS[ring][2];
                int   count       = (int) CRYSTAL_RINGS[ring][3];
                float initPhase   = CRYSTAL_RINGS[ring][4];

                double orbitAngle = totalTime * 0.05f * orbitSpeed + initPhase;

                for (int j = 0; j < count; j++) {
                    double angle = orbitAngle + j * (Math.PI * 2.0 / count);
                    float cx = (float) (Math.cos(angle) * orbitRadius);
                    float cz = (float) (Math.sin(angle) * orbitRadius);
                    float cy = ringY;

                    float pulse = 1.0f + (float) Math.sin(totalTime * 0.2f + (ring * count + j)) * 0.1f;
                    float glowSize = 0.25f * crystalAlphaValue * scaleSetting * 3.2f * pulse;
                    int glowAlpha = (int) (160 * crystalAlphaValue * (0.4f + pulse * 0.1f));

                    matrices.pushPose();
                    matrices.translate(cx, cy, cz);
                    matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

                    Matrix4f matrix = matrices.last().pose();
                    VertexConsumer glowVertex = glowBuffer.getBuffer(createTexturedRenderType(Textures.glow));

                    float hs = glowSize / 2f;
                    glowVertex.addVertex(matrix, -hs, -hs, 0).setUv(0, 1).setColor(cr, cg, cb, glowAlpha);
                    glowVertex.addVertex(matrix, hs, -hs, 0).setUv(1, 1).setColor(cr, cg, cb, glowAlpha);
                    glowVertex.addVertex(matrix, hs, hs, 0).setUv(1, 0).setColor(cr, cg, cb, glowAlpha);
                    glowVertex.addVertex(matrix, -hs, hs, 0).setUv(0, 0).setColor(cr, cg, cb, glowAlpha);
                    matrices.popPose();

                    float crystalScale = 0.18f * crystalAlphaValue * scaleSetting;
                    drawAdvancedCrystal(matrices, crystalBuffer, cx, cy, cz, crystalScale, cr, cg, cb, (int)(220 * crystalAlphaValue), totalTime, ring * count + j);
                }
            }
            glowBuffer.endBatch();
            crystalBuffer.endBatch();
        }

        matrices.popPose();
    }

    private void drawAdvancedCrystal(PoseStack matrices, MultiBufferSource.BufferSource buffer,
                                     float x, float y, float z, float scale,
                                     int r, int g, int b, int a, float time, int index) {
        matrices.pushPose();
        matrices.translate(x, y, z);

        float selfRotation = time * 3.0f + index * 45f;
        matrices.mulPose(Axis.YP.rotationDegrees(selfRotation));
        matrices.mulPose(Axis.XP.rotationDegrees(selfRotation * 0.5f));

        matrices.scale(scale, scale, scale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(createCrystalRenderType());

        int rL = Math.min(255, (int) (r * 1.4f));
        int gL = Math.min(255, (int) (g * 1.4f));
        int bL = Math.min(255, (int) (b * 1.4f));

        int rD = (int) (r * 0.5f);
        int gD = (int) (g * 0.5f);
        int bD = (int) (b * 0.5f);

        float h = 1.0f;
        float m = 0.35f;

        addTriangle(vertex, matrix, 0, h, 0,  m, 0, m,  -m, 0, m, rL, gL, bL, a);
        addTriangle(vertex, matrix, 0, h, 0,  m, 0, -m,  m, 0, m, r, g, b, a);
        addTriangle(vertex, matrix, 0, h, 0,  -m, 0, -m,  m, 0, -m, rD, gD, bD, a);
        addTriangle(vertex, matrix, 0, h, 0,  -m, 0, m,  -m, 0, -m, r, g, b, a);

        addTriangle(vertex, matrix, 0, -h, 0,  -m, 0, m,  m, 0, m, rD, gD, bD, a);
        addTriangle(vertex, matrix, 0, -h, 0,  m, 0, m,  m, 0, -m, r, g, b, a);
        addTriangle(vertex, matrix, 0, -h, 0,  m, 0, -m,  -m, 0, -m, rL, gL, bL, a);
        addTriangle(vertex, matrix, 0, -h, 0,  -m, 0, -m,  -m, 0, m, r, g, b, a);

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
                texture + "_crystals_esp",
                RenderSetup.builder(PIPELINE_ADDITIVE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, texture)
                        .createRenderSetup()
        );
    }

    private RenderType createCrystalRenderType() {
        return RenderType.create(
                "arix_crystal_mesh",
                RenderSetup.builder(PIPELINE_CRYSTAL)
                        .bufferSize(8192)
                        .createRenderSetup()
        );
    }

    private static double easeOutCubic(double x) {
        return 1.0 - Math.pow(1.0 - x, 3.0);
    }
}