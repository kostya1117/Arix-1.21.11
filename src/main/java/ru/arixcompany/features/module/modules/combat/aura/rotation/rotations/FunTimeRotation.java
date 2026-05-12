package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.player.FallingPlayer;

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

        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackDistance);
        Vec3 directionVec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(1.0f).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(1.0f))
                .normalize();

        float baseYaw = FreeLookRepo.freeYaw;

        if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check /*&& (FallingPlayer.fromPlayer(mc.player).findFall(AttackHandler.getfalldistance()))*/) {
            tick = MathUtils.randomValue(22.0F, 28.0F);
        }

        float yawChangeSpeed   = randomLerp(25.0F, 35.0F);

        float waveA = (float) Math.cos(now / 40.0);
        float waveB = (float) Math.sin(now / 70.0);
        float yawJitter   = waveA * randomLerp(5.0F, 8.0F);
        float pitchJitter = waveB * randomLerp(8.0F,  10.0F);
        if (tick > 0.0F) {
            baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
            yawChangeSpeed = randomLerp(55.0F, 70.0F);
            waveA = (float) Math.cos(now / 25.0);
            waveB = (float) Math.sin(now / 45.0);
            yawJitter = waveA * randomLerp(3.0F, 5.0F);
            pitchJitter = waveB * randomLerp(4.0F,  8.0F);
            tick--;
        }

        float basePitch = (float) Mth.clamp(
                -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))),
                -90.0, 90.0
        );


        float pitchSpeed = randomLerp(4.0F, 8.0F);

        RotationRepo.update(
                new Rotation(baseYaw + yawJitter, basePitch + pitchJitter).normalize(),
                yawChangeSpeed,
                pitchSpeed,
                12.0F,
                12.0F,
                0,
                1,
                false
        );
    }
    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, lengthY * 0.5, 0);
    }
}
