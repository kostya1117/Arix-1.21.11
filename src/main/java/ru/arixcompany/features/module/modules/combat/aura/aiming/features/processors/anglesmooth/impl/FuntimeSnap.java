package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Interpolation;
import ru.arixcompany.utils.math.MathUtils;

public class FuntimeSnap extends FactorAngleSmooth implements IMinecraft {

    private final float idleYawSpeedMin;
    private final float idleYawSpeedMax;
    private final float idlePitchSpeedMin;
    private final float idlePitchSpeedMax;

    private final float idleYawSwingMin;
    private final float idleYawSwingMax;
    private final float idlePitchSwingMin;
    private final float idlePitchSwingMax;

    private final float burstYawSpeedMin;
    private final float burstYawSpeedMax;
    private final float burstPitchSpeedMin;
    private final float burstPitchSpeedMax;
    private final float burstYawSwingMin;
    private final float burstYawSwingMax;
    private final float burstPitchSwingMin;
    private final float burstPitchSwingMax;

    private final int burstLength;

    private int burstTicks;
    private boolean lastShouldAttack;

    public FuntimeSnap(float idleYawSpeedMin, float idleYawSpeedMax,
                       float idlePitchSpeedMin, float idlePitchSpeedMax,
                       float idleYawSwingMin, float idleYawSwingMax,
                       float idlePitchSwingMin, float idlePitchSwingMax,
                       float burstYawSpeedMin, float burstYawSpeedMax,
                       float burstPitchSpeedMin, float burstPitchSpeedMax,
                       float burstYawSwingMin, float burstYawSwingMax,
                       float burstPitchSwingMin, float burstPitchSwingMax,
                       int burstLength) {
        this.idleYawSpeedMin = idleYawSpeedMin;
        this.idleYawSpeedMax = idleYawSpeedMax;
        this.idlePitchSpeedMin = idlePitchSpeedMin;
        this.idlePitchSpeedMax = idlePitchSpeedMax;

        this.idleYawSwingMin = idleYawSwingMin;
        this.idleYawSwingMax = idleYawSwingMax;
        this.idlePitchSwingMin = idlePitchSwingMin;
        this.idlePitchSwingMax = idlePitchSwingMax;

        this.burstYawSpeedMin = burstYawSpeedMin;
        this.burstYawSpeedMax = burstYawSpeedMax;
        this.burstPitchSpeedMin = burstPitchSpeedMin;
        this.burstPitchSpeedMax = burstPitchSpeedMax;
        this.burstYawSwingMin = burstYawSwingMin;
        this.burstYawSwingMax = burstYawSwingMax;
        this.burstPitchSwingMin = burstPitchSwingMin;
        this.burstPitchSwingMax = burstPitchSwingMax;

        this.burstLength = burstLength;
    }

    public FuntimeSnap() {
        this(
                25, 35,   // idle yaw speed
                5.0f, 15.0f,     // idle pitch speed
                3.0f, 5.0f,     // idle yaw swing
                4.0f, 6.0f,    // idle pitch swing

                75.0f, 100.0f,   // burst yaw speed
                20.0f, 30.0f,   // burst pitch speed
                2.0f, 4.0f,     // burst yaw swing
                1.0f, 3.0f,     // burst pitch swing
                25              // burst ticks
        );
    }
    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawDiff = Mth.abs(Rotation.angleDifference(targetRotation.yaw(), currentRotation.yaw()));
        float pitchDiff = Mth.abs(targetRotation.pitch() - currentRotation.pitch());

        float baseYawSpeed, basePitchSpeed;

        if (burstTicks > 0) {
            baseYawSpeed = MathUtils.randomValue(burstYawSpeedMin, burstYawSpeedMax);
            basePitchSpeed = MathUtils.randomValue(burstPitchSpeedMin, burstPitchSpeedMax);
        } else {
            baseYawSpeed = MathUtils.randomValue(idleYawSpeedMin, idleYawSpeedMax);
            basePitchSpeed = MathUtils.randomValue(idlePitchSpeedMin, idlePitchSpeedMax);
        }
//
//        float hCurve = calculateFactorWithCurve(yawDiff);
//        float vCurve = calculateFactorWithCurve2(pitchDiff);
//
//        float maxYawStep = hCurve * baseYawSpeed;
//        float maxPitchStep = vCurve * basePitchSpeed;

        return new float[]{ baseYawSpeed, basePitchSpeed };
    }

    private float calculateFactorWithCurve(float rotationDifference) {
        float t = Math.min(rotationDifference / 180, 1.0f);
        float exponent = 1;
        float adjustedT = (float) Math.pow(t, exponent);

        float curve = (float) Interpolation.interpolate(
                0.0, 1.0, adjustedT,
                Interpolation.Curve.CUBIC,
                Interpolation.Mode.OUT
        );

        return Math.min(curve, 1.0f);
    }

    private float calculateFactorWithCurve2(float rotationDifference) {
        float t = Math.min(rotationDifference / 90, 1.0f);
        float exponent = 1;
        float adjustedT = (float) Math.pow(t, exponent);

        float curve = (float) Interpolation.interpolate(
                0.0, 1.0, adjustedT,
                Interpolation.Curve.CUBIC,
                Interpolation.Mode.OUT
        );

        return Math.min(curve, 1.0f);
    }


    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        boolean shouldAttack = AttackHandler.shouldAttack();

        if (shouldAttack && !lastShouldAttack) {
            burstTicks = burstLength;
        }
        lastShouldAttack = shouldAttack;

        boolean burst = burstTicks > 0;
        if (burst) burstTicks--;

        double t = Util.getNanos() / 1.0E9 * Mth.PI2 * 2.0;
        float yawJitter = (burst ? 3f : 5f) * Mth.sin(t);
        float pitchJitter = (burst ? 2f : 3f) * Mth.cos(t);

        Rotation target = burst
                ? new Rotation(targetRotation.yaw() + yawJitter, Mth.clamp(targetRotation.pitch() + pitchJitter, -90f, 90f))
                : new Rotation(RotationManager.freeYaw + yawJitter, Mth.clamp(currentRotation.pitch() + pitchJitter, -90f, 90f));

        float[] factors = calculateFactors(rotationTarget, currentRotation, target);
        return currentRotation.towardsLinear(target, factors[0], factors[1]);
    }
}
