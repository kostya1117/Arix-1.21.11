package ru.arixcompany.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.arixcompany.utils.IMinecraft;

import java.awt.*;

@UtilityClass
public class Render3dUtils implements IMinecraft {

    private final RenderPipeline FILLED_NO_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/box_filled_no_depth"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private final RenderPipeline FILLED_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/box_filled_no_depth"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private final RenderPipeline OUTLINE_NO_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/box_filled_no_depth"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private final RenderPipeline OUTLINE_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/box_filled_no_depth"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private final RenderType FILLED_NO_DEPTH = RenderType.create(
            "box_filled_no_depth",
            RenderSetup.builder(FILLED_NO_DEPTH_PIPELINE)
                    .bufferSize(1024)
                    .createRenderSetup()
    );

    private final RenderType FILLED_DEPTH = RenderType.create(
            "box_filled_depth",
            RenderSetup.builder(FILLED_DEPTH_PIPELINE)
                    .bufferSize(1024)
                    .createRenderSetup()
    );

    private final RenderType OUTLINE_NO_DEPTH = RenderType.create(
            "box_outline_no_depth",
            RenderSetup.builder(OUTLINE_NO_DEPTH_PIPELINE)
                    .bufferSize(512)
                    .createRenderSetup()
    );

    private final RenderType OUTLINE_DEPTH = RenderType.create(
            "box_outline_depth",
            RenderSetup.builder(OUTLINE_DEPTH_PIPELINE)
                    .bufferSize(512)
                    .createRenderSetup()
    );

    private static final RenderPipeline LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/world/lines"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build()
    );

    private static final RenderType LINE_RENDER_TYPE = RenderType.create(
            "arix_world_lines",
            RenderSetup.builder(LINE_PIPELINE)
                    .bufferSize(4096)
                    .createRenderSetup()
    );

    private final RenderPipeline DASHED_QUAD_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("arix", "pipeline/dashed_quad"))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private final RenderType DASHED_QUAD_TYPE = RenderType.create(
            "dashed_quad",
            RenderSetup.builder(DASHED_QUAD_PIPELINE)
                    .bufferSize(4096)
                    .createRenderSetup()
    );

    private static final RenderPipeline AURORA_BOX_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.arix("pipeline/aurora_box"))
                    .withVertexShader(Identifier.arix("core/aurora_box"))
                    .withFragmentShader(Identifier.arix("core/aurora_box"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    private static final RenderType AURORA_BOX_TYPE = RenderType.create(
            "aurora_box",
            RenderSetup.builder(AURORA_BOX_PIPELINE)
                    .bufferSize(4096)
                    .createRenderSetup()
    );

    public void renderOutline(PoseStack poseStack, AABB box, Color color, boolean depthTest) {
        renderOutline(poseStack, box, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), depthTest);
    }

    public void renderOutline(PoseStack poseStack, AABB box, int r, int g, int b, int a, boolean depthTest) {
        if (mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(512);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            RenderType renderType = depthTest ? OUTLINE_DEPTH : OUTLINE_NO_DEPTH;
            VertexConsumer vertex = buffer.getBuffer(renderType);
            Matrix4f matrix = poseStack.last().pose();

            line(vertex, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
            line(vertex, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
            line(vertex, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
            line(vertex, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

            line(vertex, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
            line(vertex, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
            line(vertex, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
            line(vertex, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

            line(vertex, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
            line(vertex, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
            line(vertex, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
            line(vertex, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);

            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    public void renderFilled(PoseStack poseStack, AABB box, Color color, boolean depthTest) {
        renderFilled(poseStack, box, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), depthTest);
    }

    public void renderFilled(PoseStack poseStack, AABB box, int r, int g, int b, int a, boolean depthTest) {
        if (mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(1024);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            RenderType renderType = depthTest ? FILLED_DEPTH : FILLED_NO_DEPTH;
            VertexConsumer vertex = buffer.getBuffer(renderType);
            Matrix4f matrix = poseStack.last().pose();

            quad(vertex, matrix,
                    minX, minY, minZ,
                    maxX, minY, minZ,
                    maxX, minY, maxZ,
                    minX, minY, maxZ,
                    r, g, b, a);

            quad(vertex, matrix,
                    minX, maxY, minZ,
                    minX, maxY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, maxY, minZ,
                    r, g, b, a);

            quad(vertex, matrix,
                    minX, minY, minZ,
                    minX, maxY, minZ,
                    maxX, maxY, minZ,
                    maxX, minY, minZ,
                    r, g, b, a);

            quad(vertex, matrix,
                    minX, minY, maxZ,
                    maxX, minY, maxZ,
                    maxX, maxY, maxZ,
                    minX, maxY, maxZ,
                    r, g, b, a);

            quad(vertex, matrix,
                    minX, minY, minZ,
                    minX, minY, maxZ,
                    minX, maxY, maxZ,
                    minX, maxY, minZ,
                    r, g, b, a);

            quad(vertex, matrix,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    maxX, maxY, maxZ,
                    maxX, minY, maxZ,
                    r, g, b, a);

            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    public void renderFilledWithOutline(PoseStack poseStack, AABB box,
                                               Color fillColor, Color outlineColor, boolean depthTest) {
        renderFilled(poseStack, box, fillColor, depthTest);
        renderOutline(poseStack, box, outlineColor, depthTest);
    }

    public void renderFilledWithOutline(PoseStack poseStack, AABB box,
                                               int fillR, int fillG, int fillB, int fillA,
                                               int outlineR, int outlineG, int outlineB, int outlineA,
                                               boolean depthTest) {
        renderFilled(poseStack, box, fillR, fillG, fillB, fillA, depthTest);
        renderOutline(poseStack, box, outlineR, outlineG, outlineB, outlineA, depthTest);
    }

    public void renderOutline(PoseStack poseStack, AABB box, int color, boolean depthTest) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        renderOutline(poseStack, box, r, g, b, a, depthTest);
    }

    public void renderFilled(PoseStack poseStack, AABB box, int color, boolean depthTest) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        renderFilled(poseStack, box, r, g, b, a, depthTest);
    }

    public void renderDashedOutline(PoseStack poseStack, AABB box, Color color,
                                    float dashLength, float gapLength, boolean depthTest) {
        if (mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        ByteBufferBuilder allocator = new ByteBufferBuilder(2048);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            RenderType renderType = depthTest ? OUTLINE_DEPTH : OUTLINE_NO_DEPTH;
            VertexConsumer vertex = buffer.getBuffer(renderType);
            Matrix4f matrix = poseStack.last().pose();

            dashedLine(vertex, matrix, minX, minY, minZ, maxX, minY, minZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, minY, minZ, maxX, minY, maxZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, minY, maxZ, minX, minY, maxZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, minX, minY, maxZ, minX, minY, minZ, dashLength, gapLength, r, g, b, a);

            dashedLine(vertex, matrix, minX, maxY, minZ, maxX, maxY, minZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, minX, maxY, maxZ, minX, maxY, minZ, dashLength, gapLength, r, g, b, a);

            dashedLine(vertex, matrix, minX, minY, minZ, minX, maxY, minZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, minY, minZ, maxX, maxY, minZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, dashLength, gapLength, r, g, b, a);
            dashedLine(vertex, matrix, minX, minY, maxZ, minX, maxY, maxZ, dashLength, gapLength, r, g, b, a);

            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    private void dashedLine(VertexConsumer vertex, Matrix4f matrix,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float dashLength, float gapLength,
                            int r, int g, int b, int a) {

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float totalLength = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (totalLength < 0.001f) return;

        float nx = dx / totalLength;
        float ny = dy / totalLength;
        float nz = dz / totalLength;

        float segmentLength = dashLength + gapLength;
        float currentPos = 0;
        boolean draw = true;

        while (currentPos < totalLength) {
            if (draw) {
                float dashEnd = Math.min(currentPos + dashLength, totalLength);

                float startX = x1 + nx * currentPos;
                float startY = y1 + ny * currentPos;
                float startZ = z1 + nz * currentPos;

                float endX = x1 + nx * dashEnd;
                float endY = y1 + ny * dashEnd;
                float endZ = z1 + nz * dashEnd;

                vertex.addVertex(matrix, startX, startY, startZ).setColor(r, g, b, a);
                vertex.addVertex(matrix, endX, endY, endZ).setColor(r, g, b, a);

                currentPos = dashEnd;
            } else {
                currentPos += gapLength;
            }
            draw = !draw;
        }
    }

    public static void renderDashedLine(PoseStack matrices, Vec3 from, Vec3 to, Color color,
                                        float dashLength, float gapLength) {
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(1024);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(LINE_RENDER_TYPE);
            Matrix4f posMatrix = matrices.last().pose();

            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int a = color.getAlpha();

            float x1 = (float) from.x;
            float y1 = (float) from.y;
            float z1 = (float) from.z;
            float x2 = (float) to.x;
            float y2 = (float) to.y;
            float z2 = (float) to.z;

            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float totalLength = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (totalLength < 0.001f) {
                buffer.endBatch();
                return;
            }

            float nx = dx / totalLength;
            float ny = dy / totalLength;
            float nz = dz / totalLength;

            float currentPos = 0;
            boolean draw = true;

            while (currentPos < totalLength) {
                if (draw) {
                    float dashEnd = Math.min(currentPos + dashLength, totalLength);

                    vertex.addVertex(posMatrix, x1 + nx * currentPos, y1 + ny * currentPos, z1 + nz * currentPos)
                            .setColor(r, g, b, a);
                    vertex.addVertex(posMatrix, x1 + nx * dashEnd, y1 + ny * dashEnd, z1 + nz * dashEnd)
                            .setColor(r, g, b, a);

                    currentPos = dashEnd;
                } else {
                    currentPos += gapLength;
                }
                draw = !draw;
            }

            buffer.endBatch();
        } finally {
            allocator.close();
        }

        matrices.popPose();
    }

    public void renderDashedOutlineThick(PoseStack poseStack, AABB box, Color color,
                                         float dashLength, float gapLength,
                                         float thickness, boolean depthTest) {
        if (mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        ByteBufferBuilder allocator = new ByteBufferBuilder(8192);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(DASHED_QUAD_TYPE);
            Matrix4f matrix = poseStack.last().pose();

            float t = thickness;

            dashedQuadLine(vertex, matrix, minX, minY, minZ, maxX, minY, minZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, minY, minZ, maxX, minY, maxZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, minY, maxZ, minX, minY, maxZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, minX, minY, maxZ, minX, minY, minZ, t, 0, r, g, b, a, dashLength, gapLength);

            dashedQuadLine(vertex, matrix, minX, maxY, minZ, maxX, maxY, minZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, t, 0, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, minX, maxY, maxZ, minX, maxY, minZ, t, 0, r, g, b, a, dashLength, gapLength);

            dashedQuadLine(vertex, matrix, minX, minY, minZ, minX, maxY, minZ, t, 1, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, minY, minZ, maxX, maxY, minZ, t, 1, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, t, 1, r, g, b, a, dashLength, gapLength);
            dashedQuadLine(vertex, matrix, minX, minY, maxZ, minX, maxY, maxZ, t, 1, r, g, b, a, dashLength, gapLength);

            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    public void renderDashedLineThick(PoseStack matrices, Vec3 from, Vec3 to, Color color,
                                      float dashLength, float gapLength, float thickness) {
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(DASHED_QUAD_TYPE);
            Matrix4f matrix = matrices.last().pose();

            float x1 = (float) from.x, y1 = (float) from.y, z1 = (float) from.z;
            float x2 = (float) to.x, y2 = (float) to.y, z2 = (float) to.z;

            float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
            float totalLen = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (totalLen < 0.001f) {
                buffer.endBatch();
                return;
            }

            int axis;
            if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
                axis = 1;
            } else {
                axis = 0;
            }

            dashedQuadLine(vertex, matrix, x1, y1, z1, x2, y2, z2, thickness, axis,
                    color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(),
                    dashLength, gapLength);

            buffer.endBatch();
        } finally {
            allocator.close();
        }

        matrices.popPose();
    }

    private void dashedQuadLine(VertexConsumer vertex, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float thickness, int axis,
                                int r, int g, int b, int a,
                                float dashLength, float gapLength) {

        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float totalLen = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (totalLen < 0.001f) return;

        float nx = dx / totalLen, ny = dy / totalLen, nz = dz / totalLen;
        float t = thickness * 0.5f;

        float ox1, oy1, oz1, ox2, oy2, oz2;

        if (axis == 1) {
            ox1 = t; oy1 = 0; oz1 = 0;
            ox2 = 0; oy2 = 0; oz2 = t;
        } else {
            ox1 = 0; oy1 = t; oz1 = 0;

            float hLen = (float) Math.sqrt(nx * nx + nz * nz);
            if (hLen > 0.001f) {
                ox2 = -nz / hLen * t;
                oy2 = 0;
                oz2 = nx / hLen * t;
            } else {
                ox2 = t; oy2 = 0; oz2 = 0;
            }
        }

        float currentPos = 0;
        boolean draw = true;

        while (currentPos < totalLen) {
            if (draw) {
                float dashEnd = Math.min(currentPos + dashLength, totalLen);

                float sx = x1 + nx * currentPos;
                float sy = y1 + ny * currentPos;
                float sz = z1 + nz * currentPos;

                float ex = x1 + nx * dashEnd;
                float ey = y1 + ny * dashEnd;
                float ez = z1 + nz * dashEnd;

                vertex.addVertex(matrix, sx - ox1, sy - oy1, sz - oz1).setColor(r, g, b, a);
                vertex.addVertex(matrix, sx + ox1, sy + oy1, sz + oz1).setColor(r, g, b, a);
                vertex.addVertex(matrix, ex + ox1, ey + oy1, ez + oz1).setColor(r, g, b, a);
                vertex.addVertex(matrix, ex - ox1, ey - oy1, ez - oz1).setColor(r, g, b, a);

                vertex.addVertex(matrix, sx - ox2, sy - oy2, sz - oz2).setColor(r, g, b, a);
                vertex.addVertex(matrix, sx + ox2, sy + oy2, sz + oz2).setColor(r, g, b, a);
                vertex.addVertex(matrix, ex + ox2, ey + oy2, ez + oz2).setColor(r, g, b, a);
                vertex.addVertex(matrix, ex - ox2, ey - oy2, ez - oz2).setColor(r, g, b, a);

                currentPos = dashEnd;
            } else {
                currentPos += gapLength;
            }
            draw = !draw;
        }
    }

    public void renderShaderFilled(PoseStack poseStack, AABB box, Color color, float alpha) {
        if (mc.level == null) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int huePacked = Mth.clamp((int) (hsb[0] * 255.0f), 0, 255);
        int satPacked = Mth.clamp((int) (hsb[1] * 255.0f), 0, 255);
        int briPacked = Mth.clamp((int) (hsb[2] * 255.0f), 0, 255);
        int alphaPacked = Mth.clamp((int) (alpha * 255.0f), 0, 255);

        ByteBufferBuilder allocator = new ByteBufferBuilder(4096);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(AURORA_BOX_TYPE);
            Matrix4f matrix = poseStack.last().pose();

            shaderQuad(vertex, matrix,
                    minX, minY, minZ,
                    maxX, minY, minZ,
                    maxX, minY, maxZ,
                    minX, minY, maxZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            shaderQuad(vertex, matrix,
                    minX, maxY, minZ,
                    minX, maxY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, maxY, minZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            shaderQuad(vertex, matrix,
                    minX, minY, minZ,
                    minX, maxY, minZ,
                    maxX, maxY, minZ,
                    maxX, minY, minZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            shaderQuad(vertex, matrix,
                    minX, minY, maxZ,
                    maxX, minY, maxZ,
                    maxX, maxY, maxZ,
                    minX, maxY, maxZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            shaderQuad(vertex, matrix,
                    minX, minY, minZ,
                    minX, minY, maxZ,
                    minX, maxY, maxZ,
                    minX, maxY, minZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            shaderQuad(vertex, matrix,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    maxX, maxY, maxZ,
                    maxX, minY, maxZ,
                    time, huePacked, satPacked, briPacked, alphaPacked);

            buffer.endBatch();
        } finally {
            allocator.close();
        }
    }

    private void shaderQuad(VertexConsumer vertex, Matrix4f matrix,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float time,
                            int r, int g, int b, int a) {
        vertex.addVertex(matrix, x1, y1, z1).setUv(0.0f, time).setColor(r, g, b, a);
        vertex.addVertex(matrix, x2, y2, z2).setUv(1.0f, time).setColor(r, g, b, a);
        vertex.addVertex(matrix, x3, y3, z3).setUv(1.0f, time).setColor(r, g, b, a);
        vertex.addVertex(matrix, x4, y4, z4).setUv(0.0f, time).setColor(r, g, b, a);
    }

    public static void renderLine(PoseStack matrices, Vec3 from, Vec3 to, Color color, float width) {
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(256);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(LINE_RENDER_TYPE);
            Matrix4f posMatrix = matrices.last().pose();

            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int a = color.getAlpha();

            vertex.addVertex(posMatrix, (float) from.x, (float) from.y, (float) from.z)
                    .setColor(r, g, b, a);

            vertex.addVertex(posMatrix, (float) to.x, (float) to.y, (float) to.z)
                    .setColor(r, g, b, a);

            buffer.endBatch();
        } finally {
            allocator.close();
        }

        matrices.popPose();
    }

    public static void renderLines(PoseStack matrices, java.util.List<LineSegment> segments) {
        if (segments.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        ByteBufferBuilder allocator = new ByteBufferBuilder(segments.size() * 32);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(allocator);

        try {
            VertexConsumer vertex = buffer.getBuffer(LINE_RENDER_TYPE);
            Matrix4f posMatrix = matrices.last().pose();

            for (LineSegment seg : segments) {
                vertex.addVertex(posMatrix, (float) seg.from.x, (float) seg.from.y, (float) seg.from.z)
                        .setColor(seg.color.getRed(), seg.color.getGreen(), seg.color.getBlue(), seg.color.getAlpha());

                vertex.addVertex(posMatrix, (float) seg.to.x, (float) seg.to.y, (float) seg.to.z)
                        .setColor(seg.color.getRed(), seg.color.getGreen(), seg.color.getBlue(), seg.color.getAlpha());
            }

            buffer.endBatch();
        } finally {
            allocator.close();
        }

        matrices.popPose();
    }

    private void line(VertexConsumer vertex, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a) {
        vertex.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        vertex.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
    }

    private void quad(VertexConsumer vertex, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int r, int g, int b, int a) {
        vertex.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        vertex.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        vertex.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        vertex.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }

    public record LineSegment(Vec3 from, Vec3 to, Color color) {}
}
