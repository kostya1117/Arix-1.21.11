package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.animation.impl.EaseOutCubic;

import java.awt.*;

public class TargetESP extends Module {

    public static final SelectSetting mode =
            new SelectSetting("Режим")
                    .value("Квадрат", "Призраки", "Кристаллы","Круг");

    private final ValueSetting crystalSize = new ValueSetting("Размер кристаллов")
            .range(0.1f, 2.0f)
            .setValue(0.8f)
            .setStep(0.1f)
            .visible(() -> mode.isSelected("Кристаллы"));

    private final ValueSetting crystalCount = new ValueSetting("Кол-во кристаллов")
            .range(8, 30)
            .setValue(20)
            .setStep(1)
            .visible(() -> mode.isSelected("Кристаллы"));

    private static final Animation alpha = new EaseInOutQuad(350, 1.0);
    private final Animation spiritsAnim = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);
    private final Animation crystalsAnim = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);

    private LivingEntity lastTarget;
    private float animationNurik = 0.0f;
    private long currentTime = System.currentTimeMillis();

    private float prevCircleStep = 0f;
    private float circleStep = 0f;

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

    private static final RenderPipeline PIPELINE_STRIP = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/triangle_strip"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    public TargetESP() {
        super("TargetESP", Category.Render);
        setup(mode, crystalSize, crystalCount);
    }

    @EventHandler
    public void onRender(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        HitAura hitAura = Arix.getInstance()
                .getModuleRepo().getModule(HitAura.class);
        if (hitAura == null) return;

        LivingEntity target = HitAura.getTarget();

        if (mode.isSelected("Квадрат")) {
            renderSquareMode(e, target);
        } else if (mode.isSelected("Призраки")) {
            renderSpiritsMode(e, target);
        } else if (mode.isSelected("Кристаллы")) {
            renderCrystalsMode(e, target);
        } else if (mode.isSelected("Круг")) {
            renderOldTargetEsp(e, target);
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mode.isSelected("Круг")) {
            prevCircleStep = circleStep;
            circleStep += 0.15f;
        }
    }

    private void renderSquareMode(EventRender3D e, LivingEntity target) {
        alpha.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (alpha.getOutput() <= 0.01f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        ByteBufferBuilder allocator = new ByteBufferBuilder(1024);

        try (allocator) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            renderSquare(e.getMatrixStack(), buffer, lastTarget, e.getTickDelta());
            buffer.endBatch();
        }
    }

    private void renderSquare(PoseStack matrices,
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

        float rawProgress = clamp01((entity.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawProgress * Math.PI);

        int r = lerpInt(baseColor.getRed(), damageColor.getRed(), hurtProgress);
        int g = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(baseColor.getBlue(), damageColor.getBlue(), hurtProgress);

        float baseScale = 1.1f;
        float finalScale = baseScale + (0.12f * hurtProgress);
        matrices.scale(finalScale, finalScale, finalScale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(texLayerND(Textures.target, PIPELINE));

        int alphaValue = (int) (255f * alpha.getOutput());

        vertex.addVertex(matrix, -0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 1);
        vertex.addVertex(matrix, 0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 1);
        vertex.addVertex(matrix, 0.5f, 0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 0);
        vertex.addVertex(matrix, -0.5f, 0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 0);

        matrices.popPose();
    }

    private void renderSpiritsMode(EventRender3D e, LivingEntity target) {
        spiritsAnim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (spiritsAnim.getOutput() <= 0.0f) {
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
        animationNurik += (4L * (now - currentTime)) / 600.0f;
        currentTime = now;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        double x = lerp(lastTarget.getX(), lastTarget.xOld, e.getTickDelta()) - cam.x;
        double y = lerp(lastTarget.getY(), lastTarget.yOld, e.getTickDelta()) - cam.y;
        double z = lerp(lastTarget.getZ(), lastTarget.zOld, e.getTickDelta()) - cam.z;

        Color color = Arix.getInstance().getCurrentTheme().getMain();
        float animVal = spiritsAnim.getOutput();
        int alphaValue = (int) (animVal * 255f);

        float rawHurt = clamp01((lastTarget.hurtTime - e.getTickDelta()) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);

        Color damageColor = new Color(255, 60, 60);
        int r = lerpInt(color.getRed(), damageColor.getRed(), hurtProgress);
        int g = lerpInt(color.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(color.getBlue(), damageColor.getBlue(), hurtProgress);

        int layers = 3;
        int particles = 12;

        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);

        try (allocator) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);
            PoseStack matrices = e.getMatrixStack();
            matrices.pushPose();

            for (int i = 0; i < layers * layers; i += layers) {
                for (int j = 0; j < particles; j++) {
                    float f2 = animationNurik + j * 0.1f;
                    float radius = 0.8f;
                    float heightOffset = 0.5f;
                    int n5 = (int) Math.pow(i, 2.0);

                    float px = radius * (float) Math.sin(f2 + n5);
                    float py = (float) (heightOffset + 0.3f * Math.sin(animationNurik + j * 0.2f) + 0.2f * i);
                    float pz = radius * (float) Math.cos(f2 - n5);

                    float particleScale = animVal * (0.006f + j / 2000.0f);

                    matrices.pushPose();
                    matrices.translate(x + px, y + py, z + pz);
                    matrices.scale(particleScale, particleScale, particleScale);
                    matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

                    Matrix4f matrix = matrices.last().pose();
                    VertexConsumer vertex = bufferSource.getBuffer(texLayerND(Textures.glow, PIPELINE_ADDITIVE));

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

    private void renderCrystalsMode(EventRender3D e, LivingEntity target) {
        crystalsAnim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (crystalsAnim.getOutput() <= 0.0f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        float easedAnim = (float) easeOutCubic(crystalsAnim.getOutput());
        float tickDelta = e.getTickDelta();

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

        Color baseColor = getCurrentColor();
        float rawHurt = clamp01((lastTarget.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);
        Color damageColor = new Color(255, 60, 60);

        int cr = lerpInt(baseColor.getRed(), damageColor.getRed(), hurtProgress);
        int cg = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int cb = lerpInt(baseColor.getBlue(), damageColor.getBlue(), hurtProgress);

        int count = crystalCount.getInt();
        float scaleSetting = crystalSize.getValue();

        PoseStack matrices = e.getMatrixStack();
        matrices.pushPose();
        matrices.translate(
                targetPos.x - cam.x,
                targetPos.y - cam.y,
                targetPos.z - cam.z
        );

        ByteBufferBuilder glowAllocator = new ByteBufferBuilder(4096);
        try (glowAllocator) {
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
                VertexConsumer vertex = glowBuffer.getBuffer(texLayerND(Textures.glow, PIPELINE_ADDITIVE));

                float hs = glowSize / 2f;
                vertex.addVertex(matrix, -hs, -hs, 0).setUv(0, 1).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, hs, -hs, 0).setUv(1, 1).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, hs, hs, 0).setUv(1, 0).setColor(cr, cg, cb, glowAlpha);
                vertex.addVertex(matrix, -hs, hs, 0).setUv(0, 0).setColor(cr, cg, cb, glowAlpha);

                matrices.popPose();
            }

            glowBuffer.endBatch();
        }

        ByteBufferBuilder crystalAllocator = new ByteBufferBuilder(8192);
        try (crystalAllocator) {
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

                drawCrystal(matrices, crystalBuffer, x, y, z, crystalScale, angle,
                        cr, cg, cb, crystalAlpha);
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
        VertexConsumer vertex = buffer.getBuffer(crystalRenderType());

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

    private RenderType crystalRenderType() {
        return RenderType.create(
                "arix_crystal_triangles",
                RenderSetup.builder(PIPELINE_CRYSTAL)
                        .bufferSize(8192)
                        .createRenderSetup()
        );
    }

    private static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    private void renderOldTargetEsp(EventRender3D e, LivingEntity target) {
        crystalsAnim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);

        if (crystalsAnim.getOutput() <= 0.0f) {
            lastTarget = null;
            return;
        }

        if (target != null) lastTarget = target;
        if (lastTarget == null) return;

        float tickDelta = e.getTickDelta();
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

        float animVal = (float) crystalsAnim.getOutput();
        float width = lastTarget.getBbWidth() * 0.8f;

        int segments = 64;

        PoseStack matrices = e.getMatrixStack();
        matrices.pushPose();

        ByteBufferBuilder allocator = new ByteBufferBuilder(segments * 2 * 32 + 512);
        try (allocator) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);
            Matrix4f matrix = matrices.last().pose();

            VertexConsumer vertex = bufferSource.getBuffer(oldEspRenderType());
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

        ByteBufferBuilder allocator2 = new ByteBufferBuilder(segments * 2 * 32 + 512);
        try (allocator2) {
            MultiBufferSource.BufferSource bufferSource2 = MultiBufferSource.immediate(allocator2);
            Matrix4f matrix = matrices.last().pose();

            VertexConsumer vertex2 = bufferSource2.getBuffer(oldEspRenderType());
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

    private RenderType oldEspRenderType() {
        return RenderType.create(
                "arix_old_target_esp",
                RenderSetup.builder(PIPELINE_STRIP)
                        .bufferSize(4096)
                        .createRenderSetup()
        );
    }


    private RenderType texLayerND(Identifier tex, RenderPipeline pipeline) {
        return RenderType.create(
                tex.toString() + "_" + pipeline.getLocation(),
                RenderSetup.builder(pipeline)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, tex)
                        .createRenderSetup()
        );
    }

    private Color getCurrentColor() {
        Color themeColor = Arix.getInstance().getCurrentTheme().getMain();

        if (lastTarget == null) return themeColor;

        float rawHurt = clamp01(lastTarget.hurtTime / 10.0f);
        if (rawHurt <= 0) return themeColor;

        Color redColor = new Color(255, 50, 50);
        int r = lerpInt(themeColor.getRed(), redColor.getRed(), rawHurt);
        int g = lerpInt(themeColor.getGreen(), redColor.getGreen(), rawHurt);
        int b = lerpInt(themeColor.getBlue(), redColor.getBlue(), rawHurt);

        return new Color(r, g, b);
    }

    private static double easeOutCubic(double x) {
        return 1.0 - Math.pow(1.0 - x, 3.0);
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private int lerpInt(int from, int to, float delta) {
        return (int) (from + (to - from) * delta);
    }

    private double lerp(double current, double old, float tickDelta) {
        return old + (current - old) * tickDelta;
    }
}