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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

import java.awt.*;
import java.util.ArrayList;

public class HitBubbles extends Module {

    private static final Identifier TEX_BUBBLE =
            Identifier.withDefaultNamespace("textures/arix/hitbubble.png");

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/hitbubble"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private final ValueSetting lifeTime = new ValueSetting("Время жизни")
            .setValue(30).range(1, 150).step(1);

    private final ArrayList<HitBubble> bubbles = new ArrayList<>();

    public HitBubbles() {
        super("HitBubbles", Category.Render);
        setup(lifeTime);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        bubbles.clear();
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (mc.player == null) return;

        Vec3 point = getHitPoint();
        if (point == null) return;

        float yaw   = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        bubbles.add(new HitBubble(
                (float) point.x,
                (float) point.y,
                (float) point.z,
                -yaw,
                pitch,
                System.currentTimeMillis()
        ));
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        long maxLife = (long) (lifeTime.getValue() * 50);
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);
        try (allocator) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            PoseStack matrices = e.getMatrixStack();

            for (HitBubble b : bubbles) {
                long passed = System.currentTimeMillis() - b.spawnTime;
                float progress = (float) passed / maxLife;
                float alpha    = 1.0f - progress;
                if (alpha <= 0) continue;

                float scale = Math.min(progress * 4f, 1.0f) * 0.35f;

                float rotation = -(float) passed / 4f;

                matrices.pushPose();
                matrices.translate(
                        b.x - cam.x,
                        b.y - cam.y,
                        b.z - cam.z
                );

                matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());

                matrices.mulPose(Axis.ZP.rotationDegrees(rotation));

                matrices.scale(scale, scale, scale);

                Matrix4f matrix = matrices.last().pose();
                VertexConsumer vertex = buffer.getBuffer(texLayer());

                Color color = Arix.getInstance().getCurrentTheme().getMain();
                int red = color.getRed();
                int blue = color.getBlue();
                int green = color.getGreen();

                int a = (int) (alpha * 255f);
                vertex.addVertex(matrix, -1f, -1f, 0).setUv(0, 1).setColor(red, green, blue, a);
                vertex.addVertex(matrix,  1f, -1f, 0).setUv(1, 1).setColor(red, green, blue, a);
                vertex.addVertex(matrix,  1f,  1f, 0).setUv(1, 0).setColor(red, green, blue, a);
                vertex.addVertex(matrix, -1f,  1f, 0).setUv(0, 0).setColor(red, green, blue, a);

                matrices.popPose();
            }

            buffer.endBatch();
        }

        bubbles.removeIf(hitBubble -> System.currentTimeMillis() - hitBubble.spawnTime > maxLife);
    }

    private Vec3 getHitPoint() {
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) {
            return mc.hitResult.getLocation();
        }
        if (mc.player != null) {
            return mc.player.getEyePosition()
                    .add(mc.player.getViewVector(1f).scale(3.0));
        }
        return null;
    }

    private RenderType texLayer() {
        return RenderType.create(
                "hitbubble",
                RenderSetup.builder(PIPELINE)
                        .bufferSize(4096)
                        .withTexture(RenderType.SAMPLER0, TEX_BUBBLE)
                        .createRenderSetup()
        );
    }

    private record HitBubble(float x, float y, float z,
                              float yaw, float pitch,
                              long spawnTime) {}
}
