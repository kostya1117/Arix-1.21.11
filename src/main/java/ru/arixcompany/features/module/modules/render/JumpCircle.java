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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends Module {

    private static final Identifier TEX_CIRCLE =
            Identifier.withDefaultNamespace("textures/arix/circle.png");

    private static final float ROTATE_SPEED = 2f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/jump_circle"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.ADDITIVE)
                    .build()
    );

    private static final RenderType RENDER_TYPE = RenderType.create(
            "jump_circle",
            RenderSetup.builder(PIPELINE)
                    .bufferSize(4096)
                    .withTexture(RenderType.SAMPLER0, TEX_CIRCLE)
                    .createRenderSetup()
    );

    private final List<Player> cache   = new CopyOnWriteArrayList<>();
    private final List<Circle> circles = new CopyOnWriteArrayList<>();

    ValueSetting scale = new ValueSetting("Размер")
            .range(0.5F,5)
            .setValue(1)
            .setStep(0.1F);

    private final ValueSetting lifeTime = new ValueSetting("Время жизни")
            .setValue(5000)
            .range(500, 5000)
            .step(500);

    public JumpCircle() {
        super("JumpCircle", Category.Render);
        setup(scale,lifeTime);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        cache.clear();
        circles.clear();
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.level == null) return;

        Player pl = mc.player;

        if (!cache.contains(pl) && !pl.onGround()) {
            cache.add(pl);
        }

        cache.removeIf(p -> {
            if (p.onGround()) {
                circles.add(new Circle(
                        new Vec3(p.getX(),
                                Math.floor(p.getY()) + 0.001,
                                p.getZ()),
                        System.currentTimeMillis()
                ));
                return true;
            }
            return false;
        });

        circles.removeIf(c -> System.currentTimeMillis() - c.spawnTime > lifeTime.getValue());
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null || circles.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Color main = Arix.getInstance().getCurrentTheme().getMain();

        List<Circle> copy = new ArrayList<>(circles);
        Collections.reverse(copy);

        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);
        try (allocator) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            PoseStack matrices = e.getMatrixStack();

            for (Circle c : copy) {
                long passed = System.currentTimeMillis() - c.spawnTime;
                float alpha = getEasedValue((float) passed);

                float sizeAnim = scale.getValue() * alpha;

                if (sizeAnim <= 0.001f || alpha <= 0.01f) continue;

                float rotation = (float) passed * 0.1f * ROTATE_SPEED;

                matrices.pushPose();
                matrices.translate(
                        c.pos.x - cam.x,
                        c.pos.y - cam.y,
                        c.pos.z - cam.z
                );
                matrices.mulPose(Axis.XP.rotationDegrees(90f));
                matrices.mulPose(Axis.ZP.rotationDegrees(rotation));

                Matrix4f matrix = matrices.last().pose();
                VertexConsumer vertex = buffer.getBuffer(RENDER_TYPE);

                int r = main.getRed();
                int g = main.getGreen();
                int b = main.getBlue();
                int a = (int)(alpha * 255f);

                vertex.addVertex(matrix, -sizeAnim, sizeAnim, 0).setUv(0, 1).setColor(r, g, b, a);
                vertex.addVertex(matrix, sizeAnim, sizeAnim, 0).setUv(1, 1).setColor(r, g, b, a);
                vertex.addVertex(matrix, sizeAnim, -sizeAnim, 0).setUv(1, 0).setColor(r, g, b, a);
                vertex.addVertex(matrix, -sizeAnim, -sizeAnim, 0).setUv(0, 0).setColor(r, g, b, a);

                matrices.popPose();
            }
            buffer.endBatch();
        }
    }

    private float getEasedValue(float passed) {
        long life   = (long) lifeTime.getValue();
        float progress = passed / life;

        float transitionIn = 0.2f;
        float transitionOut = 0.2f;
        float animState;

        if (progress < transitionIn) {
            animState = progress / transitionIn;
        } else if (progress > (1f - transitionOut)) {
            animState = (1f - progress) / transitionOut;
        } else {
            animState = 1f;
        }

        return 1f - (float) Math.pow(1f - animState, 4);
    }

    private record Circle(Vec3 pos, long spawnTime) {}
}
