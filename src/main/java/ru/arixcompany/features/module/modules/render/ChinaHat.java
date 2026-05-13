package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CameraType;
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
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.Colors;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChinaHat extends Module {

    // ── Pipelines ─────────────────────────────────────────────────────────────

    /** Заливка конуса — TRIANGLES */
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

    /** Контур конуса — DEBUG_LINES */
    private static final RenderPipeline PIPELINE_OUTLINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/chinahat_outline"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderType RT_FILL = RenderType.create(
            "chinahat_fill",
            RenderSetup.builder(PIPELINE_FILL).bufferSize(8192).createRenderSetup()
    );

    private static final RenderType RT_OUTLINE = RenderType.create(
            "chinahat_outline",
            RenderSetup.builder(PIPELINE_OUTLINE).bufferSize(4096).createRenderSetup()
    );

    // ── Настройки ─────────────────────────────────────────────────────────────

    private final ListSetting targets = new ListSetting("Показывать")
            .value("Себе", "Друзьям", "Остальным")
            .selected("Себе", "Друзьям", "Остальным");

    public ChinaHat() {
        super("ChinaHat", Category.Render);
        setup(targets);
    }

    // ── Рендер ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        float   td          = e.getTickDelta();
        Vec3    cam         = mc.gameRenderer.getMainCamera().position();

        Color main   = Arix.getInstance().getCurrentTheme().getMain();
        Color friend = new Color(Colors.friend(1f));

        List<Player> players = new ArrayList<>(mc.level.players());

        ByteBufferBuilder allocator = new ByteBufferBuilder(8192);
        try (allocator) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);
            PoseStack matrices = e.getMatrixStack();

            for (Player player : players) {
                boolean isSelf   = player == mc.player;
                boolean isFriend = FriendRepo.isFriend(player);

                if (isSelf   && !targets.isSelected("Себе"))     continue;
                if (isFriend && !targets.isSelected("Друзьям"))  continue;
                if (!isSelf && !isFriend && !targets.isSelected("Остальным")) continue;
                if (isSelf && firstPerson) continue;

                Color hatColor = (isFriend && !isSelf) ? friend : main;

                // Интерполированная позиция
                double px = Mth.lerp(td, player.xo, player.getX());
                double py = Mth.lerp(td, player.yo, player.getY());
                double pz = Mth.lerp(td, player.zo, player.getZ());

                // Прикрепляем к верхушке головы (как в оригинале: y + height)
                double hatX = px - cam.x;
                double hatY = py + player.getBbHeight() - cam.y;
                double hatZ = pz - cam.z;

                matrices.pushPose();
                matrices.translate(hatX, hatY, hatZ);

                drawConeHat(matrices, buffer, hatColor);

                matrices.popPose();
            }

            buffer.endBatch();
        }
    }

    // ── Геометрия ─────────────────────────────────────────────────────────────

    private static final int   SEGMENTS    = 32;
    private static final float CONE_RADIUS = 0.4f;
    private static final float CONE_HEIGHT = 0.6f;

    /**
     * Рисует конус:
     * 1. Боковые грани (TRIANGLES)
     * 2. Дно (TRIANGLES — triangle fan вручную)
     * 3. Контур основания (DEBUG_LINES)
     */
    private void drawConeHat(PoseStack matrices, MultiBufferSource.BufferSource buffer, Color color) {
        Matrix4f matrix = matrices.last().pose();

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = 180;          // основная прозрачность заливки
        int aBottom = 120;    // дно чуть темнее
        int aOutline = 255;   // контур полностью непрозрачный

        // ── 1. Боковые грани конуса ───────────────────────────────────────────
        VertexConsumer fill = buffer.getBuffer(RT_FILL);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float)(2 * Math.PI * i       / SEGMENTS);
            float a1 = (float)(2 * Math.PI * (i + 1) / SEGMENTS);

            float x0 = CONE_RADIUS * Mth.cos(a0);
            float z0 = CONE_RADIUS * Mth.sin(a0);
            float x1 = CONE_RADIUS * Mth.cos(a1);
            float z1 = CONE_RADIUS * Mth.sin(a1);

            // Вершина конуса → два угла основания
            fill.addVertex(matrix, x0, 0,           z0).setColor(r, g, b, a);
            fill.addVertex(matrix, x1, 0,           z1).setColor(r, g, b, a);
            fill.addVertex(matrix, 0,  CONE_HEIGHT, 0 ).setColor(r, g, b, a);
        }

        // ── 2. Дно конуса (triangle fan вручную) ─────────────────────────────
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float)(2 * Math.PI * i       / SEGMENTS);
            float a1 = (float)(2 * Math.PI * (i + 1) / SEGMENTS);

            float x0 = CONE_RADIUS * Mth.cos(a0);
            float z0 = CONE_RADIUS * Mth.sin(a0);
            float x1 = CONE_RADIUS * Mth.cos(a1);
            float z1 = CONE_RADIUS * Mth.sin(a1);

            fill.addVertex(matrix, 0,  0, 0 ).setColor(r, g, b, aBottom);
            fill.addVertex(matrix, x1, 0, z1).setColor(r, g, b, aBottom);
            fill.addVertex(matrix, x0, 0, z0).setColor(r, g, b, aBottom);
        }

        // ── 3. Контур основания (line loop вручную через DEBUG_LINES) ─────────
        VertexConsumer outline = buffer.getBuffer(RT_OUTLINE);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float)(2 * Math.PI * i / SEGMENTS);
            float x = CONE_RADIUS * Mth.cos(angle);
            float z = CONE_RADIUS * Mth.sin(angle);
            outline.addVertex(matrix, x, 0, z).setColor(r, g, b, aOutline);
        }

        // Рёбра от основания к вершине (4 штуки для объёма)
        for (int i = 0; i < 4; i++) {
            float angle = (float)(2 * Math.PI * i / 4);
            float x = CONE_RADIUS * Mth.cos(angle);
            float z = CONE_RADIUS * Mth.sin(angle);
            outline.addVertex(matrix, x, 0,           z).setColor(r, g, b, aOutline);
            outline.addVertex(matrix, 0, CONE_HEIGHT, 0).setColor(r, g, b, aOutline);
        }
    }
}
