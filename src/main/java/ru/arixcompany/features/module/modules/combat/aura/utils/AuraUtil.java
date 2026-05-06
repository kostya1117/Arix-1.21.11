package ru.arixcompany.features.module.modules.combat.aura.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import ru.arixcompany.utils.IMinecraft;

public final class AuraUtil implements IMinecraft {

    public static double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public static boolean validDistance(Entity entity, float distance, boolean smart) {
        return getStrictDistance(entity) < distance;
    }

    public static Vec3 getClosestVec(Entity entity) {
        Vec3 eyePos = mc.player.getEyePosition();
        return getClosestVec(eyePos, entity).subtract(eyePos);
    }

    public static Vec3 getClosestVec(Vec3 vec, AABB aabb) {
        return new Vec3(
                Mth.clamp(vec.x, aabb.minX, aabb.maxX),
                Mth.clamp(vec.y, aabb.minY, aabb.maxY),
                Mth.clamp(vec.z, aabb.minZ, aabb.maxZ)
        );
    }

    public static Vec3 getClosestVec(Vec3 vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) rotationYaw += 180.0F;

        float forward = 1.0F;
        if (moveForward < 0.0F) forward = -0.5F;
        else if (moveForward > 0.0F) forward = 0.5F;

        if (moveStrafing > 0.0F) rotationYaw -= 90.0F * forward;
        if (moveStrafing < 0.0F) rotationYaw += 90.0F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static Vec3 getClosestTargetPoint(Vec3 vec, Entity entity, float inflate) {
        if (entity == null) return Vec3.ZERO;

        AABB box = entity.getBoundingBox().inflate(-inflate);
        Vec3 center = box.getCenter();
        Vec3 closestPoint = null;
        double closestDistance = Double.MAX_VALUE;

        for (double offsetX = 0.0; offsetX <= (box.maxX - box.minX) / 2.0; offsetX += 0.1) {
            for (double offsetY = 0.0; offsetY <= (box.maxY - box.minY) / 2.0; offsetY += 0.1) {
                for (double offsetZ = 0.0; offsetZ <= (box.maxZ - box.minZ) / 2.0; offsetZ += 0.1) {
                    for (int signX : new int[]{-1, 1}) {
                        for (int signY : new int[]{-1, 1}) {
                            for (int signZ : new int[]{-1, 1}) {
                                Vec3 point = new Vec3(
                                        center.x + signX * offsetX,
                                        center.y + signY * offsetY,
                                        center.z + signZ * offsetZ
                                );
                                Vec2 rotation = calculate(point);
                                if (RayTraceUtil.calculateRayTrace(6.0, rotation.x, rotation.y, mc.player, false)
                                        instanceof EntityHitResult hit && hit.getEntity().equals(entity)) {
                                    double dist = vec.distanceTo(point);
                                    if (dist < closestDistance) {
                                        closestDistance = dist;
                                        closestPoint = point;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (closestPoint != null) return closestPoint;

        return new Vec3(
                Mth.clamp(vec.x, box.minX, box.maxX),
                Mth.clamp(vec.y, box.minY, box.maxY),
                Mth.clamp(vec.z, box.minZ, box.maxZ)
        );
    }

    public static Vec2 calculate(Vec3 toVec) {
        return calculate(mc.player.getEyePosition(), toVec);
    }

    public static Vec2 calculate(Vec3 fromVec, Vec3 toVec) {
        Vec3 diff = toVec.subtract(fromVec);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distance));
        return new Vec2(yaw, pitch);
    }

    public static Vec3 getClosestTargetPoint(Entity entity) {
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return getClosestTargetPoint(
                mc.player.getEyePosition(tickDelta),
                entity,
                Math.min(entity.getBbWidth(), entity.getBbHeight()) / 4.0F
        );
    }

    public static Vector4f calculateRotationFromCamera(LivingEntity target) {
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eyePos = mc.player.getEyePosition(tickDelta);
        Vec3 vec = getClosestTargetPoint(target).subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) -Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z)));

        float yawDelta = Mth.wrapDegrees(rawYaw - mc.player.getYRot());
        float pitchDelta = rawPitch - mc.player.getXRot();
        return new Vector4f(rawYaw, rawPitch, yawDelta, pitchDelta);
    }

    public static double calculateFOVFromCamera(LivingEntity target) {
        Vector4f rotation = calculateRotationFromCamera(target);
        float yawDelta = rotation.z();
        float pitchDelta = rotation.w();
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
}
