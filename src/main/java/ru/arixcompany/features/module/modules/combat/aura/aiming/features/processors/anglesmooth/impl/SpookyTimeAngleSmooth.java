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
import ru.arixcompany.utils.math.Randomizer;

public class SpookyTimeAngleSmooth extends FactorAngleSmooth implements IMinecraft {

    private final float yawSpeedMin;
    private final float yawSpeedMax;
    private final Randomizer randomizer = new Randomizer();

    public SpookyTimeAngleSmooth(float yawSpeedMin, float yawSpeedMax) {
        this.yawSpeedMin = yawSpeedMin;
        this.yawSpeedMax = yawSpeedMax;
    }

    public SpookyTimeAngleSmooth() {
        this(44, 50);
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

        float hCurve = calculateFactorWithCurve(yawDiff, baseYawSpeed);
        float vCurve = calculateFactorWithCurve(pitchDiff, basePitchSpeed);

        float maxYawStep = hCurve * baseYawSpeed;
        float maxPitchStep = vCurve * basePitchSpeed;

        return new float[]{ maxYawStep, maxPitchStep };
    }

    private float calculateFactorWithCurve(float rotationDifference, float speed) {
        float t = Math.min(rotationDifference / 180, 1.0f);

        float speedInfluence = Mth.clamp(speed, -1, 1);

        float adjustedT = (float) Math.pow(t, 1.0 / speedInfluence);
        adjustedT = Math.min(adjustedT, 1.0f);

        float curve = (float) Interpolation.interpolate(
                0.0, 1.0, adjustedT,
                Interpolation.Type.SINE,
                Interpolation.Ease.OUT
        );

        return Math.min(curve, 1.0f);
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        double t = Util.getNanos() / 1.0E9 * Mth.PI2 * 2.0;
        float yawJitter = 6f * Mth.sin(t);

        Rotation jitteredTarget = new Rotation(
                targetRotation.yaw() + yawJitter,
                Mth.clamp(targetRotation.pitch(), -90f, 90f)
        );

        float[] factors = calculateFactors(rotationTarget, currentRotation, jitteredTarget);
        return currentRotation.towardsLinear(jitteredTarget, factors[0], factors[1]);
    }

    private float computePitchAccel() {
        LivingEntity target = HitAura.target;
        if (target == null) return 1.0f;

        float range = HitAura.attackRange.getValue() + HitAura.preRange.getValue();
return AttackHandler.anyEntityOnRay(new Rotation(mc.player), target, range)
                ? 1
                : randomizer.nextFloat(2, 4);
    }
}