package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.AngleSmooth;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

/**
 * SpookyTime — синусоидальные смещения yaw/pitch, движение к цели через clamp angleDifference.
 */
public class SpookyTimeAngleSmooth extends AngleSmooth implements IMinecraft {

    private final float yawSpeedMin;
    private final float yawSpeedMax;

    private final float yawSwingMin;
    private final float yawSwingMax;
    private final float pitchSwingMin;
    private final float pitchSwingMax;

    public SpookyTimeAngleSmooth(float yawSpeedMin, float yawSpeedMax,
                                  float yawSwingMin, float yawSwingMax,
                                  float pitchSwingMin, float pitchSwingMax) {
        this.yawSpeedMin   = yawSpeedMin;
        this.yawSpeedMax   = yawSpeedMax;
        this.yawSwingMin   = yawSwingMin;
        this.yawSwingMax   = yawSwingMax;
        this.pitchSwingMin = pitchSwingMin;
        this.pitchSwingMax = pitchSwingMax;
    }

    public SpookyTimeAngleSmooth() {
        this(9f, 11f, 5f, 10f, 3f, 6f);
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawSpeed   = MathUtils.randomValue(yawSpeedMin, yawSpeedMax);
        float pitchSpeed = computePitchAccel();

        double t = Util.getNanos() / 1.0E9 * Math.TAU * 2.0;
        float sin = (float) Math.sin(t);
        float cos = (float) Math.cos(t);

        float yawSwing   = MathUtils.randomValue(yawSwingMin, yawSwingMax) * sin;
        float pitchSwing = MathUtils.randomValue(pitchSwingMin, pitchSwingMax) * cos;

        float targetYaw   = targetRotation.yaw()   + yawSwing;
        float targetPitch = Mth.clamp(targetRotation.pitch() + pitchSwing, -90f, 90f);

        float diffYaw   = Rotation.angleDifference(targetYaw,   currentRotation.yaw());
        float diffPitch = Rotation.angleDifference(targetPitch, currentRotation.pitch());

        float newYaw   = currentRotation.yaw()   + Mth.clamp(diffYaw,   -yawSpeed,   yawSpeed);
        float newPitch = Mth.clamp(currentRotation.pitch() + Mth.clamp(diffPitch, -pitchSpeed, pitchSpeed), -90f, 90f);

        return new Rotation(newYaw, newPitch);
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        float diffYaw   = Math.abs(Rotation.angleDifference(targetRotation.yaw(),   currentRotation.yaw()));
        float diffPitch = Math.abs(Rotation.angleDifference(targetRotation.pitch(), currentRotation.pitch()));
        float ticksYaw   = diffYaw   / Math.max(15, 0.001f);
        float ticksPitch = diffPitch / Math.max(15, 0.001f);
        return (int) Math.ceil(Math.max(ticksYaw, ticksPitch));
    }

    private float computePitchAccel() {
        try {
            LivingEntity target = HitAura.target;
            if (target == null || RotationManager.currentRotation == null) {
                return MathUtils.randomValue(0.5f, 2f);
            }
            float range = HitAura.attackRange.getValue() + HitAura.preRange.getValue();
            return AttackHandler.anyEntityOnRay(target, range)
                ? 0.5f
                : MathUtils.randomValue(0.5f, 2f);
        } catch (Exception e) {
            return 1f;
        }
    }
}
