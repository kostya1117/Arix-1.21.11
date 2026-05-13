package ru.arixcompany.features.module.modules.render;

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
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.Render3dUtils;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PearlPrediction extends Module {

    private final List<PearlPoint> pearlPoints = new ArrayList<>();
    private static final ItemStack PEARL_STACK = new ItemStack(Items.ENDER_PEARL);

    public PearlPrediction() {
        super("PearlPrediction", Category.Render);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        pearlPoints.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) continue;

            Vec3 motion = pearl.getDeltaMovement();
            Vec3 pos = pearl.position();
            int ticks = 0;

            String owner = pearl.getOwner().getName().getString();

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
                }

                trajectory.add(pos);

                if (hitResult.getType() == HitResult.Type.BLOCK || pos.y < -128.0) {
                    pearlPoints.add(new PearlPoint(pos, ticks, owner));
                    break;
                }
                ticks++;
            }

            Color mainColor = Arix.getInstance().getCurrentTheme().getMain();
            for (int i = 1; i < trajectory.size(); i++) {
                float fade = Mth.clamp(i / 25.0f, 0.0f, 1.0f);
                int alpha = (int) (255.0f * fade);
                Color lineColor = new Color(
                        mainColor.getRed(),
                        mainColor.getGreen(),
                        mainColor.getBlue(),
                        alpha
                );

                Render3dUtils.renderLine(
                        e.getMatrixStack(),
                        trajectory.get(i - 1),
                        trajectory.get(i),
                        lineColor,
                        3.0f
                );
            }
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.level == null || mc.player == null) return;

        float fontSize = 13;
        float nameFontSize = 14;

        for (PearlPoint point : pearlPoints) {
            Vec3 screen = ProjectUtils.worldSpaceToScreenSpace(
                    new Vec3(point.position.x, point.position.y - 0.3, point.position.z)
            );
            if (screen == null || screen.z < 0 || screen.z > 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            double time = point.ticks * 50.0 / 1000.0;
            String timeText = String.format(Locale.US, "%.1fсек", time);
            String nameText = point.ownerName;

            int nameColor;
            boolean isTarget = HitAura.getTarget() != null &&
                    HitAura.getTarget().getName().getString().equals(point.ownerName);
            boolean isFriend = FriendRepo.isFriend(point.ownerName);
            boolean isPlayer = mc.player.getName().getString().equals(point.ownerName);

            if (isTarget) {
                nameColor = 0xFFFF5555;
            } else if (isFriend || isPlayer) {
                nameColor = 0xFF55FF55;
            } else {
                nameColor = 0xFFFFFFFF;
            }

            float nameWidth = FontManager.get(nameFontSize).getWidth(nameText);
            float timeWidth = FontManager.get(fontSize).getWidth(timeText);

            float iconSize = 11;
            float iconPadding = 3;
            float padding = 5;
            float verticalSpacing = 2;

            float nameRectW = padding + iconSize + iconPadding + nameWidth + padding;
            float nameRectH = 16;

            float timeRectW = padding + timeWidth + padding;
            float timeRectH = 14;

            float nameRectX = sx - nameRectW / 2f;
            float nameRectY = sy - nameRectH - verticalSpacing / 2f;

            float timeRectX = sx - timeRectW / 2f;
            float timeRectY = sy + verticalSpacing / 2f;

            RenderUtils.fillRoundRect(
                    e.getGuiGraphics(),
                    nameRectX, nameRectY,
                    nameRectW, nameRectH,
                    4f, 0xB2060712
            );

            float pearlX = nameRectX + padding;
            float pearlY = nameRectY + nameRectH / 2f - iconSize / 2f;

            e.getGuiGraphics().pose().pushMatrix();
            e.getGuiGraphics().pose().translate(pearlX, pearlY);
            float scale = iconSize / 16f;
            e.getGuiGraphics().pose().scale(scale, scale);
            e.getGuiGraphics().renderItem(PEARL_STACK, 0, 0);
            e.getGuiGraphics().pose().popMatrix();

            FontManager.get(nameFontSize).drawString(
                    e.getGuiGraphics(), nameText,
                    nameRectX + padding + iconSize + iconPadding,
                    nameRectY + nameRectH / 2f - FontManager.get(nameFontSize).getHeight() / 2f,
                    nameColor
            );

            RenderUtils.fillRoundRect(
                    e.getGuiGraphics(),
                    timeRectX, timeRectY,
                    timeRectW, timeRectH,
                    4f, 0xB2060712
            );

            FontManager.get(fontSize).drawString(
                    e.getGuiGraphics(), timeText,
                    timeRectX + padding,
                    timeRectY + timeRectH / 2f - FontManager.get(fontSize).getHeight() / 2f,
                    -1
            );
        }
    }

    private Vec3 getNextMotion(ThrowableProjectile throwable, Vec3 prevPos, Vec3 motion) {        boolean inWater = mc.level.getBlockState(BlockPos.containing(prevPos))
                .getFluidState()
                .is(FluidTags.WATER);

        motion = inWater ? motion.scale(0.8) : motion.scale(0.99);

        if (!throwable.isNoGravity()) {
            motion = motion.add(0.0, -0.03, 0.0);
        }

        return motion;
    }

    private record PearlPoint(Vec3 position, int ticks, String ownerName) {
    }
}