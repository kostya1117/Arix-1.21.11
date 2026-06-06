package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Interpolation;
import ru.arixcompany.utils.math.Randomizer;

public class ElytraAngleSmooth extends FactorAngleSmooth implements IMinecraft {

    private final float yawSpeedMin;
    private final float yawSpeedMax;
    private final float pitchSpeedMin;
    private final float pitchSpeedMax;
    private final Randomizer randomizer = new Randomizer();

    public ElytraAngleSmooth(float yawSpeedMin, float yawSpeedMax,float pitchSpeedMin, float pitchSpeedMax) {
        this.yawSpeedMin = yawSpeedMin;
        this.yawSpeedMax = yawSpeedMax;
        this.pitchSpeedMin = pitchSpeedMin;
        this.pitchSpeedMax = pitchSpeedMax;
    }

    public ElytraAngleSmooth() {
        this(150, 200,50,80);
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawDiff = Mth.abs(Rotation.angleDifference(targetRotation.yaw(), currentRotation.yaw()));
        float pitchDiff = Mth.abs(targetRotation.pitch() - currentRotation.pitch());

        float baseYawSpeed = randomizer.nextFloat(yawSpeedMin, yawSpeedMax);
        float basePitchSpeed = randomizer.nextFloat(pitchSpeedMin, pitchSpeedMax);

//        float hCurve = calculateFactorWithCurve(yawDiff, baseYawSpeed);
//        float vCurve = calculateFactorWithCurvePitch(pitchDiff, basePitchSpeed);

//        float maxYawStep = hCurve * baseYawSpeed;
//        float maxPitchStep = vCurve * basePitchSpeed;

        return new float[]{ baseYawSpeed, basePitchSpeed };
    }

    private float calculateFactorWithCurve(float rotationDifference, float speed) {
        float t = Math.min(rotationDifference / 180, 1.0f);

        float speedInfluence = Mth.clampedLerp(speed, -1, 1);

        float adjustedT = (float) Math.pow(t, 1.0 / speedInfluence);
        adjustedT = Math.min(adjustedT, 1.0f);

        float curve = (float) Interpolation.interpolate(
                0.0, 1.0, adjustedT,
                Interpolation.Type.LINEAR,
                Interpolation.Ease.OUT
        );

        return Math.min(curve, 1.0f);
    }
    private float calculateFactorWithCurvePitch(float rotationDifference, float speed) {
        float t = Math.min(rotationDifference / 90, 1.0f);

        float speedInfluence = Mth.clampedLerp(speed, -1, 1);

        float adjustedT = (float) Math.pow(t, 1.0 / speedInfluence);
        adjustedT = Math.min(adjustedT, 1.0f);

        float curve = (float) Interpolation.interpolate(
                0.0, 1.0, adjustedT,
                Interpolation.Type.LINEAR,
                Interpolation.Ease.OUT
        );

        return Math.min(curve, 1.0f);
    }
}