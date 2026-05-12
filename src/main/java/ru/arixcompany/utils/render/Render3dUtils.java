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
