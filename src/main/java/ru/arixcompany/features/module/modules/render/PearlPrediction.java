package ru.arixcompany.features.module.modules.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.Render3dUtils;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class PearlPrediction extends Module {

    private final List<PearlPoint> pearlPoints = new CopyOnWriteArrayList<>();
    private static final ItemStack PEARL_STACK = new ItemStack(Items.ENDER_PEARL);

    public PearlPrediction() {
        super("PearlPrediction", Category.Render);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        List<PearlPoint> currentPoints = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) continue;

            Vec3 pos = new Vec3(
                    Mth.lerp(e.getTickDelta(), pearl.xo, pearl.getX()),
                    Mth.lerp(e.getTickDelta(), pearl.yo, pearl.getY()),
                    Mth.lerp(e.getTickDelta(), pearl.zo, pearl.getZ())
            );

            Vec3 motion = pearl.getDeltaMovement();
            int ticks = 0;

            List<Vec3> trajectory = new ArrayList<>();
            trajectory.add(pos);

            for (int i = 0; i < 150; i++) {
                Vec3 prevPos = pos;
                pos = pos.add(motion);
                motion = getNextMotion(pearl, prevPos, motion);

                HitResult hitResult = mc.level.clip(
                        new ClipContext(
                                prevPos, pos,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                pearl
                        )
                );

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    pos = hitResult.getLocation();
                    trajectory.add(pos);
                    currentPoints.add(new PearlPoint(pos, ticks));
                    break;
                }

                trajectory.add(pos);
                if (pos.y < -64.0) break;
                ticks++;
            }

            Color mainColor = Arix.getInstance().getCurrentTheme().getMain();
            for (int i = 1; i < trajectory.size(); i++) {
                float alphaMultiplier = 1.0f - ((float) i / trajectory.size());
                Color lineColor = new Color(
                        mainColor.getRed(),
                        mainColor.getGreen(),
                        mainColor.getBlue(),
                        (int) (255 * alphaMultiplier)
                );

                Render3dUtils.renderLine(e.getMatrixStack(), trajectory.get(i - 1), trajectory.get(i), lineColor, 2.0f);
            }
        }

        pearlPoints.clear();
        pearlPoints.addAll(currentPoints);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.level == null || mc.player == null) return;

        float fontSize = 11;

        for (PearlPoint point : pearlPoints) {
            Vec3 screen = ProjectUtils.worldSpaceToScreenSpace(point.position);
            if (screen == null || screen.z < 0 || screen.z > 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            double time = point.ticks * 0.05;
            String timeText = String.format(Locale.US, "%.1fсек", time);

            float timeWidth = FontManager.get(fontSize).getWidth(timeText);
            float iconSize = 10;
            float padding = 5;

            float rectW = iconSize + timeWidth + (padding * 2.5f);
            float rectH = 14;

            float rx = sx - rectW / 2f;
            float ry = sy - rectH - 10;

            RenderUtils.fillRoundRect(e.getGuiGraphics(), rx, ry, rectW, rectH, 3f, new Color(0, 0, 0, 160).getRGB());
            renderItemIcon(e.getGuiGraphics(), rx + padding, ry + rectH / 2f - iconSize / 2f, iconSize);

            FontManager.get(fontSize).drawString(
                    e.getGuiGraphics(),
                    timeText,
                    rx + padding + iconSize + 2,
                    ry + rectH / 2f - FontManager.get(fontSize).getHeight() / 2f,
                    -1
            );
        }
    }

    private void renderItemIcon(GuiGraphics guiGraphics, float x, float y, float size) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        float scale = size / 16f;
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.renderItem(PEARL_STACK, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    private Vec3 getNextMotion(ThrowableProjectile throwable, Vec3 prevPos, Vec3 motion) {
        boolean inWater = mc.level.getFluidState(BlockPos.containing(prevPos)).is(FluidTags.WATER);
        float drag = inWater ? 0.8f : 0.99f;
        motion = motion.scale(drag);
        if (!throwable.isNoGravity()) {
            motion = motion.add(0.0, -0.03, 0.0);
        }
        return motion;
    }

    private record PearlPoint(Vec3 position, int ticks) {}
}