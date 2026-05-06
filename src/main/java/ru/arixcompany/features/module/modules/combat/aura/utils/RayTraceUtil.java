package ru.arixcompany.features.module.modules.combat.aura.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.utils.IMinecraft;

public final class RayTraceUtil implements IMinecraft {

    public static HitResult calculateRayTrace(
            double distance,
            float yaw,
            float pitch,
            Entity entity,
            boolean ignoreBlocks
    ) {
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 start = mc.player.getEyePosition(tickDelta);
        Vec3 direction = getVectorForRotation(pitch, yaw);
        Vec3 end = start.add(direction.scale(distance));

        HitResult blockResult = traceBlock(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE);
        double blockDistSqr = blockResult.getLocation().distanceToSqr(start);

        AABB box = entity.getBoundingBox()
                .expandTowards(direction.scale(distance))
                .inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                entity,
                start,
                end,
                box,
                e -> !e.isSpectator() && e.isAlive() && e.isPickable(),
                distance * distance
        );

        if (entityHit == null) return blockResult;

        double entityDistSqr = entityHit.getLocation().distanceToSqr(start);

        if (!ignoreBlocks && entityDistSqr >= blockDistSqr) return blockResult;

        return entityHit;
    }

    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity) {
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eye = mc.player.getEyePosition(tickDelta);
        // ИСПРАВЛЕНО: getVectorForRotation(pitch, yaw) — правильный порядок
        Vec3 look = getVectorForRotation(pitch, yaw);
        Vec3 end = eye.add(look.scale(distance));

        AABB box = entity.getBoundingBox();
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float yawRad   = (float) Math.toRadians(-yaw)   - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);

        float cosYaw   = (float) Math.cos(yawRad);
        float sinYaw   = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public static HitResult traceBlock(
            Vec3 start,
            Vec3 end,
            ClipContext.Block blockMode,
            ClipContext.Fluid fluidMode
    ) {
        return mc.level.clip(new ClipContext(start, end, blockMode, fluidMode, mc.player));
    }
}
