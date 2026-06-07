package ru.arixcompany.features.module.modules.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.render.Render3dUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BlockHighLight extends Module {

    private final SelectSetting mode = new SelectSetting("Режим")
            .value("Заливка", "Заливка (сторона)",
                    "Контур", "Контур (сторона)",
                    "Пунктир", "Пунктир (сторона)",
                    "Шейдер")
            .selected("Контур");

    public BlockHighLight() {
        super("BlockHighLight", Category.Render);
        setup(mode);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);

        List<AABB> boxes = new ArrayList<>();
        if (!shape.isEmpty()) {
            for (AABB box : shape.toAabbs()) {
                boxes.add(box.move(pos));
            }
        }
        if (boxes.isEmpty()) {
            boxes.add(new AABB(pos));
        }

        Direction side = bhr.getDirection();
        Vec3 hit = bhr.getLocation();

        Color main = Arix.getInstance().getCurrentTheme().getMain();
        Color outline = new Color(main.getRed(), main.getGreen(), main.getBlue(), 255);
        Color fill = new Color(main.getRed(), main.getGreen(), main.getBlue(), 40);

        switch (mode.getSelected()) {
            case "Заливка" -> renderShapeFill(e, boxes, fill);
            case "Заливка (сторона)" -> renderHitSideFill(e, boxes, side, hit, fill);
            case "Контур" -> renderShapeOutline(e, boxes, outline);
            case "Контур (сторона)" -> renderHitSideOutline(e, boxes, side, hit, outline);
            case "Пунктир" -> renderShapeDashed(e, boxes, outline);
            case "Пунктир (сторона)" -> renderHitSideDashed(e, boxes, side, hit, outline);
            case "Шейдер" -> renderShapeShader(e, boxes, main);
        }
    }

    private void renderShapeShader(EventRender3D e, List<AABB> boxes, Color color) {
        for (AABB box : boxes) {
            Render3dUtils.renderShaderFilled(e.getMatrixStack(), box, color, 0.9f);
        }
    }

    private void renderShapeOutline(EventRender3D e, List<AABB> boxes, Color color) {
        for (AABB box : boxes) {
            Render3dUtils.renderOutline(e.getMatrixStack(), box, color, false);
        }
    }

    private void renderShapeFill(EventRender3D e, List<AABB> boxes, Color color) {
        for (AABB box : boxes) {
            Render3dUtils.renderFilled(e.getMatrixStack(), box, color, false);
        }
    }

    private void renderShapeDashed(EventRender3D e, List<AABB> boxes, Color color) {
        for (AABB box : boxes) {
            Render3dUtils.renderDashedOutlineThick(
                    e.getMatrixStack(), box, color,
                    0.12f, 0.1f, 0.02f, false
            );
        }
    }

    private void renderHitSideDashed(EventRender3D e, List<AABB> boxes, Direction side, Vec3 hit, Color color) {
        for (AABB box : getHitFaceBoxes(boxes, side, hit)) {
            Vec3[] corners = getSideCorners(box, side);
            for (int i = 0; i < 4; i++) {
                Render3dUtils.renderDashedLineThick(
                        e.getMatrixStack(),
                        corners[i],
                        corners[(i + 1) % 4],
                        color,
                        0.12f, 0.1f, 0.02f
                );
            }
        }
    }

    private void renderHitSideOutline(EventRender3D e, List<AABB> boxes, Direction side, Vec3 hit, Color color) {
        for (AABB box : getHitFaceBoxes(boxes, side, hit)) {
            Vec3[] corners = getSideCorners(box, side);
            for (int i = 0; i < 4; i++) {
                Render3dUtils.renderLine(e.getMatrixStack(), corners[i], corners[(i + 1) % 4], color, 1f);
            }
        }
    }

    private void renderHitSideFill(EventRender3D e, List<AABB> boxes, Direction side, Vec3 hit, Color color) {
        for (AABB box : getHitFaceBoxes(boxes, side, hit)) {
            AABB sideBox = getSideAABB(box, side);
            Render3dUtils.renderFilled(e.getMatrixStack(), sideBox, color, false);
        }
    }

    private List<AABB> getHitFaceBoxes(List<AABB> boxes, Direction side, Vec3 hit) {
        double eps = 1.0E-3;
        List<AABB> exact = new ArrayList<>();

        for (AABB box : boxes) {
            if (isHitOnFace(box, hit, side, eps)) {
                exact.add(box);
            }
        }
        if (!exact.isEmpty()) return exact;

        List<AABB> projected = new ArrayList<>();
        double bestDist = Double.MAX_VALUE;

        for (AABB box : boxes) {
            if (!isInsideFaceProjection(box, hit, side, 0.02)) continue;
            double dist = faceDistance(box, hit, side);
            if (dist < bestDist - eps) {
                projected.clear();
                projected.add(box);
                bestDist = dist;
            } else if (Math.abs(dist - bestDist) <= 0.02) {
                projected.add(box);
            }
        }
        if (!projected.isEmpty()) return projected;

        AABB nearest = null;
        bestDist = Double.MAX_VALUE;
        for (AABB box : boxes) {
            double dist = faceDistance(box, hit, side);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = box;
            }
        }

        List<AABB> fallback = new ArrayList<>();
        if (nearest != null) fallback.add(nearest);
        return fallback;
    }

    private boolean isHitOnFace(AABB box, Vec3 hit, Direction side, double eps) {
        return switch (side) {
            case UP -> Math.abs(hit.y - box.maxY) <= eps
                    && between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
            case DOWN -> Math.abs(hit.y - box.minY) <= eps
                    && between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
            case NORTH -> Math.abs(hit.z - box.minZ) <= eps
                    && between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.y, box.minY, box.maxY, eps);
            case SOUTH -> Math.abs(hit.z - box.maxZ) <= eps
                    && between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.y, box.minY, box.maxY, eps);
            case WEST -> Math.abs(hit.x - box.minX) <= eps
                    && between(hit.y, box.minY, box.maxY, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
            case EAST -> Math.abs(hit.x - box.maxX) <= eps
                    && between(hit.y, box.minY, box.maxY, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
        };
    }

    private boolean isInsideFaceProjection(AABB box, Vec3 hit, Direction side, double eps) {
        return switch (side) {
            case UP, DOWN -> between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
            case NORTH, SOUTH -> between(hit.x, box.minX, box.maxX, eps)
                    && between(hit.y, box.minY, box.maxY, eps);
            case WEST, EAST -> between(hit.y, box.minY, box.maxY, eps)
                    && between(hit.z, box.minZ, box.maxZ, eps);
        };
    }

    private double faceDistance(AABB box, Vec3 hit, Direction side) {
        return switch (side) {
            case UP -> Math.abs(hit.y - box.maxY);
            case DOWN -> Math.abs(hit.y - box.minY);
            case NORTH -> Math.abs(hit.z - box.minZ);
            case SOUTH -> Math.abs(hit.z - box.maxZ);
            case WEST -> Math.abs(hit.x - box.minX);
            case EAST -> Math.abs(hit.x - box.maxX);
        };
    }

    private boolean between(double value, double min, double max, double eps) {
        return value >= min - eps && value <= max + eps;
    }

    private Vec3[] getSideCorners(AABB b, Direction side) {
        return switch (side) {
            case UP -> new Vec3[]{
                    new Vec3(b.minX, b.maxY, b.minZ), new Vec3(b.maxX, b.maxY, b.minZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ), new Vec3(b.minX, b.maxY, b.maxZ)};
            case DOWN -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ), new Vec3(b.minX, b.minY, b.maxZ),
                    new Vec3(b.maxX, b.minY, b.maxZ), new Vec3(b.maxX, b.minY, b.minZ)};
            case NORTH -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ), new Vec3(b.maxX, b.minY, b.minZ),
                    new Vec3(b.maxX, b.maxY, b.minZ), new Vec3(b.minX, b.maxY, b.minZ)};
            case SOUTH -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.maxZ), new Vec3(b.minX, b.maxY, b.maxZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ), new Vec3(b.maxX, b.minY, b.maxZ)};
            case WEST -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ), new Vec3(b.minX, b.maxY, b.minZ),
                    new Vec3(b.minX, b.maxY, b.maxZ), new Vec3(b.minX, b.minY, b.maxZ)};
            case EAST -> new Vec3[]{
                    new Vec3(b.maxX, b.minY, b.minZ), new Vec3(b.maxX, b.minY, b.maxZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ), new Vec3(b.maxX, b.maxY, b.minZ)};
        };
    }

    private AABB getSideAABB(AABB b, Direction side) {
        double e = 0.001;
        return switch (side) {
            case UP -> new AABB(b.minX, b.maxY - e, b.minZ, b.maxX, b.maxY + e, b.maxZ);
            case DOWN -> new AABB(b.minX, b.minY - e, b.minZ, b.maxX, b.minY + e, b.maxZ);
            case NORTH -> new AABB(b.minX, b.minY, b.minZ - e, b.maxX, b.maxY, b.minZ + e);
            case SOUTH -> new AABB(b.minX, b.minY, b.maxZ - e, b.maxX, b.maxY, b.maxZ + e);
            case WEST -> new AABB(b.minX - e, b.minY, b.minZ, b.minX + e, b.maxY, b.maxZ);
            case EAST -> new AABB(b.maxX - e, b.minY, b.minZ, b.maxX + e, b.maxY, b.maxZ);
        };
    }
}