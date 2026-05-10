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

    /**
     * Точная копия серверного метода getHitResultOnViewVector из 1.21.1
     * Сервер использует именно этот метод при проверке атаки
     */
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
        Vec3 rotation = getRotationVector(yaw, pitch); // Твоя исправленная версия из RayTraceUtil
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

        // 1.21.1: Entity первым аргументом, double последним (maxDistSq), без Level!
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player,  // shooter
                start,      // startVec
                end,        // endVec
                box,        // aabb
                e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity,
                maxDistSq   // double - квадрат расстояния!
        );

        return hit != null ? hit.getEntity() : null;
    }

    /**
     * Внутренний метод сервера, воспроизводим точно
     */
    private static HitResult getHitResult(Vec3 start, Entity shooter, Predicate<Entity> predicate, Vec3 movement, Level level, float margin, ClipContext.Block blockMode) {
        Vec3 end = start.add(movement);

        // Сервер использует clipIncludingBorder (важно для границ чанков)
        HitResult blockHit = level.clipIncludingBorder(new ClipContext(start, end, blockMode, ClipContext.Fluid.NONE, shooter));

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // ВАЖНО: используем перегрузку БЕЗ Level, с Entity как первый аргумент
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                shooter,          // shooter (Entity)
                start,            // startVec
                end,              // endVec
                shooter.getBoundingBox().expandTowards(movement).inflate(1.0), // aabb
                predicate,        // predicate
                blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation()) // maxDistSq
        );

        return entityHit != null ? entityHit : blockHit;
    }

    /**
     * Критически важный метод для обхода HitBox check.
     * Проверяет, что луч пересекает хитбокс ТОЧНО так же, как это проверяет сервер.
     */
    public static boolean checkRtx(float yaw, float pitch, double distance, double wallDistance) {
        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = start.add(rotation.scale(distance));

        // Проверка блоков (COLLIDER - точно как на сервере)
        HitResult blockHit = mc.level.clipIncludingBorder(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        double maxDistSq = distance * distance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistSq = start.distanceToSqr(blockHit.getLocation());
        }

        HitAura aura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        LivingEntity target = aura.getTarget();

        if (target == null) return false;

        // Точная сигнатура 1.21.1: (Entity, Vec3, Vec3, AABB, Predicate, double)
        // double - это maxDistSq (квадрат расстояния), не путать с margin!
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player,        // shooter
                start,            // start
                end,              // end
                mc.player.getBoundingBox().expandTowards(rotation.scale(distance)).inflate(1.0, 1.0, 1.0),
                e -> e == target && !e.isSpectator() && e.isPickable(),
                maxDistSq         // максимальное расстояние (квадрат)
        );

        if (entityHit != null) {
            double entityDistSq = start.distanceToSqr(entityHit.getLocation());

            // Проверка видимости: либо ближе блока, либо в пределах wallDistance
            // +0.001 для компенсации погрешности float на сервере
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

    /**
     * Прямая проверка пересечения с AABB (для double-check перед атакой)
     * Использует тот же метод aabb.clip() что и античит
     */
    public static boolean isLookingAtAABB(float yaw, float pitch, Entity entity, double maxDistance) {
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 rotation = getRotationVector(yaw, pitch);
        Vec3 end = start.add(rotation.scale(maxDistance));

        // Проверяем пересечение луча с хитбоксом (без margin, только сам хитбокс)
        Optional<Vec3> hitPoint = entity.getBoundingBox().clip(start, end);
        return hitPoint.isPresent();
    }

    /**
     * ИСПРАВЛЕНО: Точная формула из Entity.calculateViewVector() 1.21.1
     *
     * Minecraft использует:
     * yaw: 0 = +Z, 90 = -X, 180 = -Z, 270 = +X
     * pitch: -90 = вверх (небо), 90 = вниз (ноги)
     */
    public static @NotNull Vec3 getRotationVector(float yaw, float pitch) {
        float pitchRad = pitch * ((float)Math.PI / 180F);
        float yawRad = -yaw * ((float)Math.PI / 180F);

        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);

        return new Vec3(
                sinYaw * cosPitch,   // X
                -sinPitch,           // Y (инвертирован!)
                cosYaw * cosPitch    // Z
        );
    }
}