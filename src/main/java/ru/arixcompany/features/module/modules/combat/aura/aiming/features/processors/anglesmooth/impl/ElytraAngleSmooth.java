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
        this(999, 360,30,50);
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawDiff = Mth.abs(Rotation.angleDifference(targetRotation.yaw(), currentRotation.yaw()));
        float pitchDiff = Mth.abs(targetRotation.pitch() - currentRotation.pitch());

        float baseYawSpeed = 255;
        float basePitchSpeed = 120;
        return new float[]{ baseYawSpeed, basePitchSpeed};
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float[] factors = calculateFactors(rotationTarget, currentRotation, targetRotation);
        return currentRotation.towardsLinearElytra(targetRotation, factors[0], factors[1]);
    }
}