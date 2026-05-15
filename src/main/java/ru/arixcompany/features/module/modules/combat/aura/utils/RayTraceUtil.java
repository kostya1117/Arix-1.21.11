package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.utils.IMinecraft;

import java.util.Optional;
import java.util.function.Predicate;

@UtilityClass
public final class RayTraceUtil implements IMinecraft {
    public static HitResult getServerHitResult(Entity entity, Predicate<Entity> predicate, double distance) {
        Vec3 viewVector = getRotationVector(entity.getYRot(), entity.getXRot()).scale(distance);
        Vec3 eyePos = entity.getEyePosition(1.0f);
        return getHitResult(eyePos, entity, predicate, viewVector, entity.level(), 0.0F, ClipContext.Block.COLLIDER);
    }
    public static HitResult getServerHitResult(Entity entity,float yaw,float pitch, Predicate<Entity> predicate, double distance) {
        Vec3 viewVector = getRotationVector(yaw, pitch).scale(distance);
        Vec3 eyePos = entity.getEyePosition(1.0f);
        return getHitResult(eyePos, entity, predicate, viewVector, entity.level(), 0.0F, ClipContext.Block.COLLIDER);
    }
    public static Entity getRtxTarget(float yaw, float pitch, double distance, boolean ignoreWalls) {
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 end = start.add(rotation.scale(distance));

        double maxDistSq = distance * distance;

        if (!ignoreWalls) {
            HitResult blockHit = mc.level.clipIncludingBorder(
                    new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)
            );
            if (blockHit.getType() != HitResult.Type.MISS) {
                maxDistSq = start.distanceToSqr(blockHit.getLocation());
            }
        }

        AABB box = mc.player.getBoundingBox()
                .expandTowards(rotation.scale(distance))
                .inflate(1.0, 1.0, 1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player,
                start,
                end,
                box,
                e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity,
                maxDistSq
        );

        return hit != null ? hit.getEntity() : null;
    }

    private static HitResult getHitResult(Vec3 start, Entity shooter, Predicate<Entity> predicate, Vec3 movement, Level level, float margin, ClipContext.Block blockMode) {
        Vec3 end = start.add(movement);

        HitResult blockHit = level.clipIncludingBorder(new ClipContext(start, end, blockMode, ClipContext.Fluid.NONE, shooter));

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                shooter,
                start,
                end,
                shooter.getBoundingBox().expandTowards(movement).inflate(1.0),
                predicate,
                blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation()) // maxDistSq
        );

        return entityHit != null ? entityHit : blockHit;
    }

    public static boolean checkRtx(float yaw, float pitch, double distance, double wallDistance) {
        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = start.add(rotation.scale(distance));

        HitResult blockHit = mc.level.clipIncludingBorder(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        double maxDistSq = distance * distance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistSq = start.distanceToSqr(blockHit.getLocation());
        }

        HitAura aura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        LivingEntity target = aura.getTarget();

        if (target == null) return false;

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player,
                start,
                end,
                mc.player.getBoundingBox().expandTowards(rotation.scale(distance)).inflate(1.0, 1.0, 1.0),
                e -> e == target && !e.isSpectator() && e.isPickable(),
                maxDistSq
        );

        if (entityHit != null) {
            double entityDistSq = start.distanceToSqr(entityHit.getLocation());

            boolean visible = entityDistSq <= maxDistSq + 0.001 ||
                    entityDistSq <= wallDistance * wallDistance;

            return visible;
        }

        return false;
    }

    public static boolean checkRtx(float yaw, float pitch, double distance, double wallDistance, Entity entity) {
        if (entity == null) return false;

        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = start.add(rotation.scale(distance));

        HitResult blockHit = mc.level.clipIncludingBorder(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double blockDistSq = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation());

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player,
                start,
                end,
                mc.player.getBoundingBox().expandTowards(rotation.scale(distance)).inflate(1.0),
                e -> e == entity && !e.isSpectator() && e.isPickable(),
                Math.max(blockDistSq, wallDistance * wallDistance)
        );

        return entityHit != null;
    }

    public static boolean isLookingAtAABB(float yaw, float pitch, Entity entity, double maxDistance) {
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 end = start.add(rotation.scale(maxDistance));

        Optional<Vec3> hitPoint = entity.getBoundingBox().clip(start, end);
        return hitPoint.isPresent();
    }

    public static @NotNull Vec3 getRotationVector(float yaw, float pitch) {
        float pitchRad = pitch * ((float)Math.PI / 180F);
        float yawRad = -yaw * ((float)Math.PI / 180F);

        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);

        return new Vec3(
                sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
        );
    }
}