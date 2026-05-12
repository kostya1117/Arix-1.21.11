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
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.animation.impl.EaseOutCubic;

import java.awt.*;

public class TargetESP extends Module {

    // ─── Текстуры ────────────────────────────────────────────────────────────
    private static final Identifier TEX_SQUARE =
            Identifier.withDefaultNamespace("textures/arix/target.png");
    private static final Identifier TEX_GLOW =
            Identifier.withDefaultNamespace("textures/arix/glow.png");

    // ─── Настройки ───────────────────────────────────────────────────────────
    public static final SelectSetting mode =
            new SelectSetting("Режим")
                    .value("Квадрат", "Призраки");

    private static final Animation alpha = new EaseInOutQuad(350, 1.0);
    private final Animation spiritsAnim = new EaseOutCubic(400, 1.0, Direction.BACKWARDS);

    // ─── Состояние ───────────────────────────────────────────────────────────
    private LivingEntity lastTarget;
    private float animationNurik = 0.0f;
    private long currentTime = System.currentTimeMillis();

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

    public TargetESP() {
        super("TargetESP", Category.Render);
        setup(mode);
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
        } else {
            renderSpiritsMode(e, target);
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

        Color baseColor  = Arix.getInstance().getCurrentTheme().getMain();
        Color damageColor = new Color(255, 60, 60);


        float rawProgress = clamp01((entity.hurtTime - tickDelta) / 10.0f);
        float hurtProgress = (float) Math.sin(rawProgress * Math.PI);

        int r = lerpInt(baseColor.getRed(),   damageColor.getRed(),   hurtProgress);
        int g = lerpInt(baseColor.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(baseColor.getBlue(),  damageColor.getBlue(),  hurtProgress);

        // При ударе спрайт чуть увеличивается (punch эффект), потом возвращается
        float baseScale  = 1.1f;
        float finalScale = baseScale + (0.12f * hurtProgress);
        matrices.scale(finalScale, finalScale, finalScale);

        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertex = buffer.getBuffer(texLayerND(TEX_SQUARE, PIPELINE));

        int alphaValue = (int) (255f * alpha.getOutput());

        vertex.addVertex(matrix, -0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 1);
        vertex.addVertex(matrix,  0.5f, -0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 1);
        vertex.addVertex(matrix,  0.5f,  0.5f, 0).setColor(r, g, b, alphaValue).setUv(1, 0);
        vertex.addVertex(matrix, -0.5f,  0.5f, 0).setColor(r, g, b, alphaValue).setUv(0, 0);

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

        // hurtTime: sin-дуга, пик в момент удара, плавное затухание
        float rawHurt = clamp01((lastTarget.hurtTime - e.getTickDelta()) / 10.0f);
        float hurtProgress = (float) Math.sin(rawHurt * Math.PI);

        Color damageColor = new Color(255, 60, 60);
        int r = lerpInt(color.getRed(),   damageColor.getRed(),   hurtProgress);
        int g = lerpInt(color.getGreen(), damageColor.getGreen(), hurtProgress);
        int b = lerpInt(color.getBlue(),  damageColor.getBlue(),  hurtProgress);

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
                    VertexConsumer vertex = bufferSource.getBuffer(texLayerND(TEX_GLOW, PIPELINE_ADDITIVE));

                    vertex.addVertex(matrix,-19, 19, 0).setUv(0, 1).setColor(r, g, b, alphaValue);
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

    private RenderType texLayerND(Identifier tex, RenderPipeline pipeline) {
        return RenderType.create(
                tex.toString() + "_" + pipeline.getLocation(),
                RenderSetup.builder(pipeline)
                        .bufferSize(4096)
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

    private double lerp(double current, double old, float tickDelta) {
        return old + (current - old) * tickDelta;
    }
}
