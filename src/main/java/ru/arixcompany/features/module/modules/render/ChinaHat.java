package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis; // Нужен для вращения
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
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.Colors;
import com.mojang.blaze3d.platform.DepthTestFunction;

import java.awt.*;

public class ChinaHat extends Module {

    // ... (старые PIPELINE и RenderType оставляем без изменений)
    private static final RenderPipeline PIPELINE_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_fill"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline PIPELINE_BOTTOM = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_bottom"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline PIPELINE_OUTLINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_outline"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderType RT_FILL = RenderType.create("chinahat_fill", RenderSetup.builder(PIPELINE_FILL).bufferSize(8192).createRenderSetup());
    private static final RenderType RT_BOTTOM = RenderType.create("chinahat_bottom", RenderSetup.builder(PIPELINE_BOTTOM).bufferSize(4096).createRenderSetup());
    private static final RenderType RT_OUTLINE = RenderType.create("chinahat_outline", RenderSetup.builder(PIPELINE_OUTLINE).bufferSize(4096).createRenderSetup());

    private final ListSetting targets = new ListSetting("Показывать")
            .value("Себе", "Друзьям", "Остальным")
            .selected("Себе", "Друзьям", "Остальным");

    private final ValueSetting width = new ValueSetting("Ширина").range(0.2f, 3.0f).setValue(0.7f).step(0.1f);
    private final ValueSetting height = new ValueSetting("Высота").range(0.05f, 0.8f).setValue(0.45f).step(0.05f);
    private final ValueSetting alpha = new ValueSetting("Прозрачность").range(0.0f, 1.0f).setValue(0.45f).step(0.05f);

    public ChinaHat() {
        super("ChinaHat", Category.Render);
        setup(targets, width, height, alpha);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float td = e.getTickDelta();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        Color main = Arix.getInstance().getCurrentTheme().getMain();

        ByteBufferBuilder allocator = new ByteBufferBuilder(8192);
        try (allocator) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            PoseStack matrices = e.getMatrixStack();

            for (Player player : mc.level.players()) {
                boolean isSelf = player == mc.player;
                boolean isFriend = FriendRepo.isFriend(player);

                if (isSelf && !targets.isSelected("Себе")) continue;
                if (isFriend && !targets.isSelected("Друзьям")) continue;
                if (!isSelf && !isFriend && !targets.isSelected("Остальным")) continue;
                if (isSelf && firstPerson) continue;

                float w = width.getValue();
                float h = height.getValue();
                float a = alpha.getValue();

                Color hatColor = (isFriend && !isSelf) ? new Color(Colors.friend(1f)) : main;

                double px = Mth.lerp(td, player.xo, player.getX());
                double py = Mth.lerp(td, player.yo, player.getY());
                double pz = Mth.lerp(td, player.zo, player.getZ());

                float yaw = Mth.lerp(td, player.yHeadRotO, player.getYHeadRot());
                float pitch = Mth.lerp(td, player.xRotO, player.getXRot());

                double hatX = px - cam.x;
                double hatY = py + (player.isCrouching() ? player.getBbHeight() - 0.23D : player.getBbHeight()) - cam.y;
                double hatZ = pz - cam.z;

                matrices.pushPose();

                matrices.translate(hatX, hatY + 0.05, hatZ);
                matrices.mulPose(Axis.YP.rotationDegrees(-yaw));
                matrices.mulPose(Axis.XP.rotationDegrees(pitch / 2f));

                drawConeHat(matrices, buffer, hatColor, w, h, a);

                matrices.popPose();
            }

            buffer.endBatch();
        }
    }

    private static final int SEGMENTS = 32;

    private void drawConeHat(PoseStack matrices, MultiBufferSource.BufferSource buffer, Color color,
                             float radius, float coneHeight, float alpha) {
        Matrix4f matrix = matrices.last().pose();

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = (int) (alpha * 255f);

        VertexConsumer fill = buffer.getBuffer(RT_FILL);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (2 * Math.PI * i / SEGMENTS);
            float a1 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);

            float x0 = radius * Mth.cos(a0);
            float z0 = radius * Mth.sin(a0);
            float x1 = radius * Mth.cos(a1);
            float z1 = radius * Mth.sin(a1);

            fill.addVertex(matrix, x0, 0, z0).setColor(r, g, b, a);
            fill.addVertex(matrix, x1, 0, z1).setColor(r, g, b, a);
            fill.addVertex(matrix, 0, coneHeight, 0).setColor(r, g, b, a);
        }

        VertexConsumer bottom = buffer.getBuffer(RT_BOTTOM);
        bottom.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float x = radius * Mth.cos(angle);
            float z = radius * Mth.sin(angle);
            bottom.addVertex(matrix, x, 0, z).setColor(r, g, b, a);
        }

        VertexConsumer outline = buffer.getBuffer(RT_OUTLINE);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float x = radius * Mth.cos(angle);
            float z = radius * Mth.sin(angle);
            outline.addVertex(matrix, x, 0, z).setColor(r, g, b, 255);
        }
    }
}