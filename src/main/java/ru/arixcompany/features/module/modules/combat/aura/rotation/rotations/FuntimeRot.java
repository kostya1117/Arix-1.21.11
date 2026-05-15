package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookRepo;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

import java.util.concurrent.ThreadLocalRandom;

public class FuntimeRot implements AbstractRotation, IMinecraft {

    // Счётчик тиков активного прицеливания (как в FunTimeRotation)
    private float aimTick = 0f;

    private static float randomLerp(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        if (target == null || mc.player == null) return;

        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = AuraUtil.getStrictDistance(target) / attackDistance;

        Vec3 dir = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(1f).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(1f))
                .normalize();

        float rawYaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float rawPitch = (float) Mth.clamp(
                -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))),
                -90.0, 90.0
        );

        // Волны для естественного дрожания
        double t = Util.getNanos() / 1.0E9;
        float waveA = (float) Math.sin(t * Math.TAU * 1.5);
        float waveB = (float) Math.cos(t * Math.TAU * 2.0);

        Rotation targetRot = new Rotation(rawYaw, rawPitch);
        Rotation currentRot = new Rotation(mc.player.getYRot(), mc.player.getXRot());

        // Насколько далеко мы от цели
        Rotation.RotationDelta delta = currentRot.rotationDeltaTo(targetRot);
        // Смотрим ли мы уже достаточно близко к цели (погрешность 15°)
        boolean lookingAtTarget = currentRot.approximatelyEquals(targetRot, 15f);

        // Базовые параметры (свободный взгляд)
        float baseYaw     = FreeLookRepo.freeYaw;
        float yawSpeed    = randomLerp(10f, 18f);
        float pitchSpeed  = randomLerp(4f, 7f);
        float yawJitter   = waveA * randomLerp(4f, 7f);
        float pitchJitter = waveB * randomLerp(6f, 9f);

        // При атаке — захватываем цель на несколько тиков
        if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check) {
            aimTick = randomLerp(18f, 24f);
        }

        if (aimTick > 0f) {
            baseYaw     = rawYaw;
            // Скорость зависит от дистанции до цели — чем дальше, тем быстрее поворачиваем
            float distFactor = Mth.clamp(delta.length() / 45f, 0.5f, 1.5f);
            yawSpeed    = randomLerp(40f, 55f) * distFactor;
            pitchSpeed  = randomLerp(12f, 18f) * distFactor;
            // Меньше дрожания когда уже смотрим на цель
            yawJitter   = waveA * (lookingAtTarget ? randomLerp(1f, 3f) : randomLerp(2f, 4f));
            pitchJitter = waveB * (lookingAtTarget ? randomLerp(2f, 5f) : randomLerp(4f, 7f));
            aimTick--;
        }

        Rotation rotation = new Rotation(baseYaw + yawJitter, rawPitch + pitchJitter).normalize();
        RotationRepo.update(
                rotation,
                yawSpeed,
                pitchSpeed,
                10f,
                6f,
                0,
                5,
                false
        );
    }

    private Vec3 getTargetPoint(LivingEntity entity) {
        double h = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, h * 0.45, 0);
    }
}
