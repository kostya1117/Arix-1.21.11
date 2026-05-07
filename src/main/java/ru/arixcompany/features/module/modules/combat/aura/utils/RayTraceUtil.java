package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.utils.IMinecraft;

import java.util.Optional;

@UtilityClass
public final class RayTraceUtil implements IMinecraft {

    public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance) {
//        if (rt == Aura.RayTrace.OFF)
//            return true;

        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3 startPoint = mc.player.getPosPlayer().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);

        if (result != null)
            distancePow2 = startPoint.distanceToSqr(result.getLocation());

        Vec3 rotationVector = getRotationVector(pitch, yaw).scale(distance);
        Vec3 endPoint = startPoint.add(rotationVector);

        AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);

        EntityHitResult ehr;

        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));

        if (Arix.getInstance().getModuleRepo().getModule(HitAura.class).getTarget() != null)
            ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && e == Arix.getInstance().getModuleRepo().getModule(HitAura.class).target, maxDistance);
        else
            ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable(), maxDistance);

        if (ehr != null) {
            boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
            boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;

            if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2))
                return ehr.getEntity() == Arix.getInstance().getModuleRepo().getModule(HitAura.class).getTarget() || Arix.getInstance().getModuleRepo().getModule(HitAura.class).getTarget() == null;
        }

        return false;
    }


    public boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3 startPoint = mc.player.getPosPlayer().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);

        if (result != null)
            distancePow2 = startPoint.distanceToSqr(result.getLocation());

        Vec3 rotationVector = getRotationVector(pitch, yaw).scale(distance);
        Vec3 endPoint = startPoint.add(rotationVector);

        AABB entityArea = mc.player.getBoundingBox().expandTowards(rotationVector).inflate(1.0, 1.0, 1.0);

        EntityHitResult ehr;

        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));

        ehr = ProjectileUtil.getEntityHitResult(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.isPickable() && e == entity, maxDistance);

        if (ehr != null) {
            boolean allowedWallDistance = startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.distanceToSqr(ehr.getLocation()) < distancePow2;
            boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;

            if (allowWallHit && startPoint.distanceToSqr(ehr.getLocation()) <= Math.pow(distance, 2))
                return ehr.getEntity() == entity;
        }

        return false;
    }
    public HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3 vec3d = mc.player.getEyePosition(1f);
        Vec3 vec3d2 = getRotationVector(pitch, yaw);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.level.clip(new ClipContext(vec3d, vec3d3, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    public @NotNull Vec3 getRotationVector(float yaw, float pitch) {
        return new Vec3(Mth.sin(-pitch * 0.017453292F) * Mth.cos(yaw * 0.017453292F), -Mth.sin(yaw * 0.017453292F), Mth.cos(-pitch * 0.017453292F) * Mth.cos(yaw * 0.017453292F));
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
