package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Interpolation;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.Randomizer;

import javax.swing.text.html.HTML;
import java.util.concurrent.ThreadLocalRandom;

public class SpookyTimeAngleSmooth extends FactorAngleSmooth implements IMinecraft {

    private final float yawSpeedMin;
    private final float yawSpeedMax;
    private final Randomizer randomizer = new Randomizer();

    public SpookyTimeAngleSmooth(float yawSpeedMin, float yawSpeedMax) {
        this.yawSpeedMin = yawSpeedMin;
        this.yawSpeedMax = yawSpeedMax;
    }

    public SpookyTimeAngleSmooth() {
        this(25, 45);
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawDiff = Mth.abs(Rotation.angleDifference(targetRotation.yaw(), currentRotation.yaw()));
        float pitchDiff = Mth.abs(targetRotation.pitch() - currentRotation.pitch());

        float baseYawSpeed = randomizer.nextFloat(yawSpeedMin, yawSpeedMax);
        float basePitchSpeed = computePitchAccel();

        LivingEntity target = HitAura.target;
//        if (target != null && mc.player != null) {
//            float distance = mc.player.distanceTo(target);
//            float distMultiplier = Mth.clampedLerp((distance - 0.5f) / 2.5f, 0.5f, 1.0f);
//            baseYawSpeed *= distMultiplier;
//        }

//        float hCurve = calculateFactorWithCurve(yawDiff);
//        float vCurve = calculateFactorWithCurve2(pitchDiff);
//
//        float maxYawStep = hCurve * baseYawSpeed;
//        float maxPitchStep = vCurve * basePitchSpeed;

        return new float[]{baseYawSpeed, basePitchSpeed};
    }


    private float targetYawOffset = 0f;
    private float targetPitchOffset = 0f;
    private double nextOffsetUpdateTime = 0;

    public Rotation fixDeltaNonVanillaMouse(float delta, float secondDelta) {
        float value = (float) (MathUtils.randomValue(0.6f, 1.2f) + Math.pow(MathUtils.randomValue(-0.3f, 0.3f), 3));
//        if (Math.abs(delta) > 0 && Math.abs(secondDelta) == 0) secondDelta += value;
//        if (Math.abs(secondDelta) > 0 && Math.abs(delta) == 0) delta += value;

        return new Rotation(delta + value, secondDelta + value);
    }
//
//    @Override
//    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
//        double now = System.currentTimeMillis();
//
//        if (mc.player != null && AttackHandler.anyEntityOnRay(new Rotation(mc.player.getYRot(),mc.player.getXRot(),true), HitAura.target, HitAura.attackRange.getValue() + HitAura.preRange.getValue())) { // Убедитесь, что метод rayTrace() реализован в вашем классе
//            if (now >= nextOffsetUpdateTime) {
//                float maxYawOffset = MathUtils.randomValue(10,16);
//                float maxPitchOffset = MathUtils.randomValue(20,30);
//                targetYawOffset = ThreadLocalRandom.current().nextFloat() * maxYawOffset;
//                targetPitchOffset = ThreadLocalRandom.current().nextFloat() * maxPitchOffset;
//
//                targetYawOffset = Mth.clamp(targetYawOffset, -maxYawOffset, maxYawOffset);
//                targetPitchOffset = Mth.clamp(targetPitchOffset, -maxPitchOffset, maxPitchOffset);
//
//                nextOffsetUpdateTime = now + 800 + ThreadLocalRandom.current().nextInt(500);
//            }
//        } else {
//            targetYawOffset = 0f;
//            targetPitchOffset = 0f;
//        }
//
//        Rotation jitteredTarget = new Rotation(
//                targetRotation.yaw() + targetYawOffset,
//                Mth.clamp(targetRotation.pitch() + targetPitchOffset, -90f, 90f) // Не даем сломать шею
//        );
//
//        //jitteredTarget = fixDeltaNonVanillaMouse(jitteredTarget.yaw(),jitteredTarget.pitch());
//
//        float[] factors = calculateFactors(rotationTarget, currentRotation, jitteredTarget);
//        return currentRotation.towardsLinear(jitteredTarget, factors[0], factors[1]);
//    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        double t = Util.getNanos() / 1.0E9 * Mth.PI2 * 2.0;
        float yawJitter;
        float pitchJitter;

        if (mc.player != null && AttackHandler.anyEntityOnRay(new Rotation(mc.player.getYRot(), mc.player.getXRot(), true), HitAura.target, HitAura.attackRange.getValue() + HitAura.preRange.getValue())) { // Убедитесь, что метод rayTrace() реализован в вашем классе
            yawJitter = 4 * Mth.sin(t);
            pitchJitter = 3 * Mth.cos(t);
        } else {
            yawJitter = 0f;
            pitchJitter = 0f;
        }

        Rotation jitteredTarget = new Rotation(
                targetRotation.yaw() + yawJitter,
                Mth.clamp(targetRotation.pitch() + pitchJitter, -90f, 90f)
        );

        float[] factors = calculateFactors(rotationTarget, currentRotation, jitteredTarget);
        return currentRotation.towardsLinear(jitteredTarget, factors[0], factors[1]);
    }

    private float computePitchAccel() {
        LivingEntity target = HitAura.target;
        if (target == null) return 1.0f;

        float range = HitAura.attackRange.getValue() + HitAura.preRange.getValue();
        return AttackHandler.anyEntityOnRay(new Rotation(mc.player.getYRot(),mc.player.getXRot(),true), target, range)
                ? randomizer.nextFloat(0.1f,0.6f)
                : randomizer.nextFloat(1f, 3f);
    }
}