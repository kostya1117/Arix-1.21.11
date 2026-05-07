package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.UBoxPoints;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookUtil;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

import java.util.concurrent.ThreadLocalRandom;

public class FunTimeRotation implements AbstractRotation, IMinecraft {

    private static float tick;

    private static float randomLerp(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        if (target == null || mc.player == null) return;

        long now = System.currentTimeMillis();

        if (!HitAura.isLookingUp && now - HitAura.lastLookUpTime >= HitAura.nextLookUpDelay) {
            HitAura.isLookingUp    = true;
            HitAura.lookUpStartTime = now;
            HitAura.lookUpDuration  = ThreadLocalRandom.current().nextInt(270, 390);
            HitAura.lastLookUpTime  = now;
            HitAura.nextLookUpDelay = ThreadLocalRandom.current().nextLong(10500L, 13200L);
        }

        if (HitAura.isLookingUp && now - HitAura.lookUpStartTime >= HitAura.lookUpDuration) {
            HitAura.isLookingUp = false;
        }

        boolean fastspeed = now - HitAura.lookUpStartTime >= HitAura.lookUpDuration + 60L;

        Vec3 directionVec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox())
                .subtract(mc.player.getEyePosition());

        float baseYaw = FreeLookUtil.freeYaw;

        if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check) {
            tick = MathUtils.randomValue(20.0F, 25.0F);
        }

        float yawChangeSpeed   = randomLerp(25.0F, 35.0F);
        float pitchChangeSpeed = randomLerp(0.0F,  3.5F);
        float randomAttackShift = 0.0F;

        float waveA = (float) Math.cos(now / 40.0);
        float waveB = (float) Math.sin(now / 70.0);

        if (tick > 0.0F) {
            yawChangeSpeed = randomLerp(40.0F, 55.0F);
            baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
            randomAttackShift = randomLerp(2.0F, 4.0F);
            tick--;
        }

        float basePitch = (float) Mth.clamp(
                -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))),
                -90.0, 90.0
        );

        float yawJitter   = waveA * randomLerp(9.0F, 11.0F) + randomAttackShift;
        float pitchJitter = waveB * randomLerp(5.0F,  6.0F)  + randomAttackShift;

        float finalPitch = HitAura.isLookingUp
                ? -randomLerp(85.0F, 90.0F)
                : basePitch;

        float pitchSpeed = HitAura.isLookingUp || fastspeed
                ? randomLerp(60.0F, 90.0F)
                : randomLerp(9.0F, 12.0F);

        RotationRepo.update(
                new Rotation(baseYaw + yawJitter, finalPitch + pitchJitter),
                yawChangeSpeed,
                pitchSpeed,
                8.0F,
                8.0F,
                0,
                2,
                false
        );
    }
}
