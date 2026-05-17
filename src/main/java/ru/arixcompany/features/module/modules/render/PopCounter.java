package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventTotemPop;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.features.repos.alerts.AlertRepo;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PopCounter extends Module {
    private final List<Ghost> ghosts = new CopyOnWriteArrayList<>();
    private static final float U = 1.0F / 16.0F;

    private static final RenderPipeline GHOST_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/ghost_player"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderType GHOST_LAYER = RenderType.create("ghost_player_layer",
            RenderSetup.builder(GHOST_PIPELINE)
                    .bufferSize(8192)
                    .createRenderSetup()
    );

    public PopCounter() {
        super("PopCounter", Category.Render);
        setup(alert,riseHeight, duration);
    }

    private final BooleanSetting alert = new BooleanSetting("Уведомления");
    private final ValueSetting riseHeight = new ValueSetting("Высота подъема")
            .range(0.2f, 5.0f)
            .setValue(4.0f)
            .setStep(0.1f);
    private final ValueSetting duration = new ValueSetting("Время жизни")
            .range(0.5f, 6.0f)
            .setValue(3.0f)
            .setStep(0.1f);

    @Override
    public void deactivate() {
        ghosts.clear();
        super.deactivate();
    }

    @EventHandler
    public void onTotemPop(EventTotemPop e) {
        addGhost(e.getEntity(), true);
        if (alert.isValue() && e.getEntity() != mc.player) {
            AlertRepo.success("попнул " + e.getEntity().getName().getString());
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (ghosts.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        long now = System.currentTimeMillis();
        float maxLife = duration.getValue() * 1000f;

        ByteBufferBuilder allocator = new ByteBufferBuilder(8192);
        try (allocator) {
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(allocator);
            PoseStack matrices = event.getMatrixStack();

            ghosts.removeIf(ghost -> {
                float progress = (now - ghost.startTime) / maxLife;
                if (progress >= 1.0f) return true;

                float alpha = 1.0f - progress;
                double yOffset = ghost.rising ? (riseHeight.getValue() * (1.0 - Math.pow(1.0 - progress, 3))) : 0;

                matrices.pushPose();
                matrices.translate(ghost.pos.x - cam.x, ghost.pos.y - cam.y + yOffset, ghost.pos.z - cam.z);
                matrices.mulPose(Axis.YP.rotationDegrees(180.0f - ghost.yaw));

                Color c = Arix.getInstance().getCurrentTheme().getMain();
                renderFlatHumanoid(matrices, bufferSource.getBuffer(GHOST_LAYER),
                        c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha * 0.5f,
                        ghost.sneaking, ghost.age);

                matrices.popPose();
                return false;
            });

            bufferSource.endBatch();
        }
    }

    private void renderFlatHumanoid(PoseStack matrices, VertexConsumer buffer, float r, float g, float b, float a, boolean sneaking, int age) {
        matrices.pushPose();
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0, -1.501f, 0);

        if (sneaking) {
            matrices.translate(0, 0.2f, 0);
            matrices.mulPose(Axis.XP.rotationDegrees(28.0f));
        }

        Matrix4f matrix = matrices.last().pose();

        drawBox(buffer, matrix, -4*U, 0, -2*U, 8*U, 12*U, 4*U, r, g, b, a);
        drawBox(buffer, matrix, -4*U, -8*U, -4*U, 8*U, 8*U, 8*U, r, g, b, a);

        float swing = Mth.sin(age * 0.2f) * 0.5f;

        renderPart(matrices, buffer, matrix, -6*U, 2*U, swing, true, r, g, b, a);
        renderPart(matrices, buffer, matrix, 6*U, 2*U, -swing, true, r, g, b, a);

        renderPart(matrices, buffer, matrix, -2*U, 12*U, -swing, false, r, g, b, a);
        renderPart(matrices, buffer, matrix, 2*U, 12*U, swing, false, r, g, b, a);

        matrices.popPose();
    }

    private void renderPart(PoseStack matrices, VertexConsumer buffer, Matrix4f matrix, float px, float py, float angle, boolean arm, float r, float g, float b, float a) {
        matrices.pushPose();
        matrices.translate(px, py, 0);
        matrices.mulPose(Axis.XP.rotation(angle));
        matrices.translate(-px, -py, 0);
        float bx = arm ? (px < 0 ? -8*U : 4*U) : (px < 0 ? -4*U : 0);
        drawBox(buffer, matrices.last().pose(), bx, arm ? -2*U : 12*U, -2*U, 4*U, 12*U, 4*U, r, g, b, a);
        matrices.popPose();
    }

    private void drawBox(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float a) {
        float x2 = x + sx, y2 = y + sy, z2 = z + sz;

        vertex(buffer, matrix, x, y, z2, r, g, b, a);
        vertex(buffer, matrix, x2, y, z2, r, g, b, a);
        vertex(buffer, matrix, x2, y2, z2, r, g, b, a);
        vertex(buffer, matrix, x, y2, z2, r, g, b, a);

        vertex(buffer, matrix, x2, y, z, r, g, b, a);
        vertex(buffer, matrix, x, y, z, r, g, b, a);
        vertex(buffer, matrix, x, y2, z, r, g, b, a);
        vertex(buffer, matrix, x2, y2, z, r, g, b, a);

        vertex(buffer, matrix, x, y, z, r, g, b, a);
        vertex(buffer, matrix, x, y, z2, r, g, b, a);
        vertex(buffer, matrix, x, y2, z2, r, g, b, a);
        vertex(buffer, matrix, x, y2, z, r, g, b, a);

        vertex(buffer, matrix, x2, y, z2, r, g, b, a);
        vertex(buffer, matrix, x2, y, z, r, g, b, a);
        vertex(buffer, matrix, x2, y2, z, r, g, b, a);
        vertex(buffer, matrix, x2, y2, z2, r, g, b, a);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }

    private void addGhost(Player player, boolean rising) {
        ghosts.add(new Ghost(player.position(), player.yBodyRot, player.isCrouching(), player.tickCount, rising, System.currentTimeMillis()));
    }

    @AllArgsConstructor
    private static class Ghost {
        Vec3 pos;
        float yaw;
        boolean sneaking;
        int age;
        boolean rising;
        long startTime;
    }
}