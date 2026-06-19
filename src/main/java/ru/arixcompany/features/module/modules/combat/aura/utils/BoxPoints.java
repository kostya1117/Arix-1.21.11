package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class BoxPoints implements IMinecraft {

    private static final double BOX_OFFSET = 0.01;
    private static final double DISTANCE_FACTOR = 5.0;

    public static Vec3 getBestVectorOnEntityBox(AABB aabb) {
        return getBestVectorOnEntityBox(aabb, true);
    }

    public static Vec3 getBestVectorOnEntityBox(AABB aabb, boolean multipoints) {
        LocalPlayer player = mc.player;

        if (player == null) {
            return Vec3.ZERO;
        }

        if (aabb == null) {
            return player.getEyePosition();
        }

        if (mc.level == null) {
            return getCenter(aabb);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 base = getCenter(aabb);

        if (!multipoints || isVisible(player, eye, base)) {
            return base;
        }

        Vec3 best = findClosestVisiblePoint(aabb.inflate(-BOX_OFFSET), player, eye);
        return best != null ? best : base;
    }

    private static Vec3 findClosestVisiblePoint(AABB aabb, LocalPlayer player, Vec3 eye) {
        Vec3 center = getCenter(aabb);

        double factor = 1.0 - Math.min(player.position().distanceTo(center) / DISTANCE_FACTOR, 1.0);
        int pointsXZ = lerp(5, 17, factor);
        int pointsY = lerp(6, 24, factor);

        Vec3 bestPoint = null;
        double bestDistanceSqr = Double.MAX_VALUE;

        for (int xi = 0; xi < pointsXZ; xi++) {
            double x = lerp(aabb.minX, aabb.maxX, xi / (double) (pointsXZ - 1));

            for (int zi = 0; zi < pointsXZ; zi++) {
                double z = lerp(aabb.minZ, aabb.maxZ, zi / (double) (pointsXZ - 1));

                for (int yi = 0; yi < pointsY; yi++) {
                    double y = lerp(aabb.minY, aabb.maxY, yi / (double) (pointsY - 1));

                    Vec3 point = new Vec3(x, y, z);

                    if (!isVisible(player, eye, point)) {
                        continue;
                    }

                    double distanceSqr = eye.distanceToSqr(point);
                    if (distanceSqr < bestDistanceSqr) {
                        bestDistanceSqr = distanceSqr;
                        bestPoint = point;
                    }
                }
            }
        }

        return bestPoint;
    }

    private static boolean isVisible(LocalPlayer player, Vec3 from, Vec3 to) {
        HitResult result = mc.level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        return result.getType() != HitResult.Type.BLOCK;
    }

    private static Vec3 getCenter(AABB aabb) {
        return new Vec3(
                (aabb.minX + aabb.maxX) * 0.5,
                (aabb.minY + aabb.maxY) * 0.5,
                (aabb.minZ + aabb.maxZ) * 0.5
        );
    }

    private static int lerp(int a, int b, double t) {
        return a + (int) Math.round(t * (b - a));
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
}