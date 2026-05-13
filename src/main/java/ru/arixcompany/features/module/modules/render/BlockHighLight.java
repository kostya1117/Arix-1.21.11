package ru.arixcompany.features.module.modules.render;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.render.Render3dUtils;

import java.awt.*;

public class BlockHighLight extends Module {

    private final SelectSetting mode = new SelectSetting("Режим")
            .value("Оба", "Оба (сторона)", "Заливка", "Заливка (сторона)", "Контур", "Контур (сторона)")
            .selected("Контур");

    private final ValueSetting lineWidth = new ValueSetting("Толщина")
            .setValue(1f).range(0.1f, 5f).step(0.1f);

    public BlockHighLight() {
        super("BlockHighLight", Category.Render);
        setup(mode, lineWidth);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.hitResult == null) return;
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (!(mc.hitResult instanceof BlockHitResult bhr)) return;

        AABB box = new AABB(bhr.getBlockPos());
        Direction side = bhr.getDirection();

        Color main    = Arix.getInstance().getCurrentTheme().getMain();
        Color outline = new Color(main.getRed(), main.getGreen(), main.getBlue(), 255);
        Color fill    = new Color(main.getRed(), main.getGreen(), main.getBlue(), 40);

        switch (mode.getSelected()) {
            case "Оба" -> {
                Render3dUtils.renderOutline(e.getMatrixStack(), box, outline, false);
                Render3dUtils.renderFilled(e.getMatrixStack(), box, fill, false);
            }
            case "Оба (сторона)" -> {
                renderSideOutline(e, box, side, outline);
                renderSideFill(e, box, side, fill);
            }
            case "Заливка" ->
                Render3dUtils.renderFilled(e.getMatrixStack(), box, fill, false);
            case "Заливка (сторона)" ->
                renderSideFill(e, box, side, fill);
            case "Контур" ->
                Render3dUtils.renderOutline(e.getMatrixStack(), box, outline, false);
            case "Контур (сторона)" ->
                renderSideOutline(e, box, side, outline);
        }
    }

    private void renderSideOutline(EventRender3D e, AABB box, Direction side, Color color) {
        float w = lineWidth.getValue();
        Vec3[] corners = getSideCorners(box, side);
        for (int i = 0; i < 4; i++) {
            Render3dUtils.renderLine(e.getMatrixStack(),
                    corners[i], corners[(i + 1) % 4], color, w);
        }
    }

    private void renderSideFill(EventRender3D e, AABB box, Direction side, Color color) {
        AABB sideBox = getSideAABB(box, side);
        Render3dUtils.renderFilled(e.getMatrixStack(), sideBox, color, false);
    }

    private Vec3[] getSideCorners(AABB b, Direction side) {
        return switch (side) {
            case UP    -> new Vec3[]{
                    new Vec3(b.minX, b.maxY, b.minZ),
                    new Vec3(b.maxX, b.maxY, b.minZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ),
                    new Vec3(b.minX, b.maxY, b.maxZ)};
            case DOWN  -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ),
                    new Vec3(b.minX, b.minY, b.maxZ),
                    new Vec3(b.maxX, b.minY, b.maxZ),
                    new Vec3(b.maxX, b.minY, b.minZ)};
            case NORTH -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ),
                    new Vec3(b.maxX, b.minY, b.minZ),
                    new Vec3(b.maxX, b.maxY, b.minZ),
                    new Vec3(b.minX, b.maxY, b.minZ)};
            case SOUTH -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.maxZ),
                    new Vec3(b.minX, b.maxY, b.maxZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ),
                    new Vec3(b.maxX, b.minY, b.maxZ)};
            case WEST  -> new Vec3[]{
                    new Vec3(b.minX, b.minY, b.minZ),
                    new Vec3(b.minX, b.maxY, b.minZ),
                    new Vec3(b.minX, b.maxY, b.maxZ),
                    new Vec3(b.minX, b.minY, b.maxZ)};
            case EAST  -> new Vec3[]{
                    new Vec3(b.maxX, b.minY, b.minZ),
                    new Vec3(b.maxX, b.minY, b.maxZ),
                    new Vec3(b.maxX, b.maxY, b.maxZ),
                    new Vec3(b.maxX, b.maxY, b.minZ)};
        };
    }

    private AABB getSideAABB(AABB b, Direction side) {
        double e = 0.001;
        return switch (side) {
            case UP    -> new AABB(b.minX, b.maxY - e, b.minZ, b.maxX, b.maxY + e, b.maxZ);
            case DOWN  -> new AABB(b.minX, b.minY - e, b.minZ, b.maxX, b.minY + e, b.maxZ);
            case NORTH -> new AABB(b.minX, b.minY, b.minZ - e, b.maxX, b.maxY, b.minZ + e);
            case SOUTH -> new AABB(b.minX, b.minY, b.maxZ - e, b.maxX, b.maxY, b.maxZ + e);
            case WEST  -> new AABB(b.minX - e, b.minY, b.minZ, b.minX + e, b.maxY, b.maxZ);
            case EAST  -> new AABB(b.maxX - e, b.minY, b.minZ, b.maxX + e, b.maxY, b.maxZ);
        };
    }
}
