package ru.arixcompany.features.module.modules.combat.aura.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import ru.arixcompany.utils.IMinecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UBoxPoints implements IMinecraft {

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int lerp(int a, int b, float f) {
        return a + (int)(f * (b - a));
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    public static HitResult traceBlock(Vec3 start, Vec3 end,
                                       ClipContext.Block blockMode,
                                       ClipContext.Fluid fluidMode) {
        if (mc.level == null || mc.player == null) return null;
        return mc.level.clip(new ClipContext(start, end, blockMode, fluidMode, mc.player));
    }

    private static boolean canSee(Player player, Vec3 vec) {
        if (mc.level == null) return false;
        HitResult result = traceBlock(
                player.getEyePosition(),
                vec,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE
        );
        return result == null || result.getType() != HitResult.Type.BLOCK;
    }

    public static List<Vec3> entityBoxVec3sAlternate(AABB box) {
        if (mc.player == null) return List.of();

        List<Vec3> points = new ArrayList<>();

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        int xPoints = 6;
        int yPoints = 8;
        int zPoints = 6;

        for (int xi = 0; xi <= xPoints; xi++) {
            double x = lerp(minX, maxX, (double) xi / xPoints);

            for (int zi = 0; zi <= zPoints; zi++) {
                double z = lerp(minZ, maxZ, (double) zi / zPoints);

                for (int yi = 0; yi <= yPoints; yi++) {
                    double y = lerp(minY, maxY, (double) yi / yPoints);

                    Vec3 vec = new Vec3(x, y, z);

                    if (canSee(mc.player, vec)) {
                        points.add(vec);
                    }
                }
            }
        }

        return points;
    }

    private static double distanceSqr(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b);
    }

    public static Vec3 getBestVector3dOnEntityBox(AABB box) {
        return getBestVector3dOnEntityBox(box, true);
    }

    public static Vec3 getBestVector3dOnEntityBox(AABB box, boolean multipoint) {
        if (mc.player == null) return Vec3.ZERO;

        if (box == null) return mc.player.getEyePosition();

        double centerX = (box.minX + box.maxX) / 2.0;
        double centerY = box.minY;
        double centerZ = (box.minZ + box.maxZ) / 2.0;

        double height = box.maxY - box.minY;

        double pitchHeight = clamp(height / 2.0, 0.0, height);

        Vec3 defaultVec = new Vec3(centerX, centerY + pitchHeight, centerZ);

        if (!multipoint && canSee(mc.player, defaultVec)) {
            return defaultVec;
        }

        List<Vec3> points = entityBoxVec3sAlternate(box);
        if (points.isEmpty()) return defaultVec;

        Vec3 eye = mc.player.getEyePosition();

        points.sort(Comparator.comparingDouble(vec -> distanceSqr(eye, vec)));

        return points.get(0);
    }
}