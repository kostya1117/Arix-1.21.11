/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl;

import net.minecraft.util.Mth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.RotationDelta;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.AngleSmooth;

import java.util.concurrent.ThreadLocalRandom;

public class AccelerationAngleSmooth extends AngleSmooth {

    private final float yawAccelerationMin;
    private final float yawAccelerationMax;
    private final float pitchAccelerationMin;
    private final float pitchAccelerationMax;

    private final boolean sigmoidEnabled;
    private final float sigmoidSteepness;
    private final float sigmoidMidpoint;

    private final float yawAccelError;
    private final float pitchAccelError;
    private final float yawConstantError;
    private final float pitchConstantError;

    public AccelerationAngleSmooth(float yawAccelerationMin, float yawAccelerationMax,
                                   float pitchAccelerationMin, float pitchAccelerationMax,
                                   boolean sigmoidEnabled, float sigmoidSteepness, float sigmoidMidpoint,
                                   float yawAccelError, float pitchAccelError,
                                   float yawConstantError, float pitchConstantError) {
        this.yawAccelerationMin   = yawAccelerationMin;
        this.yawAccelerationMax   = yawAccelerationMax;
        this.pitchAccelerationMin = pitchAccelerationMin;
        this.pitchAccelerationMax = pitchAccelerationMax;
        this.sigmoidEnabled       = sigmoidEnabled;
        this.sigmoidSteepness     = sigmoidSteepness;
        this.sigmoidMidpoint      = sigmoidMidpoint;
        this.yawAccelError        = yawAccelError;
        this.pitchAccelError      = pitchAccelError;
        this.yawConstantError     = yawConstantError;
        this.pitchConstantError   = pitchConstantError;
    }

    public AccelerationAngleSmooth() {
        this(20f, 25f, 20f, 25f, false, 10f, 0.3f, 0.1f, 0.1f, 0.1f, 0.1f);
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        Rotation prevRotation = RotationManager.previousRotation != null
                ? RotationManager.previousRotation
                : currentRotation;

        RotationDelta prevDiff = prevRotation.rotationDeltaTo(currentRotation);
        RotationDelta diff     = currentRotation.rotationDeltaTo(targetRotation);

        float decelerationFactor = sigmoidEnabled
                ? computeSigmoidDeceleration(diff.length())
                : 1.0f;

        float yawAccel   = computeAcceleration(diff.deltaYaw(),   prevDiff.deltaYaw(),   yawAccelerationMin,   yawAccelerationMax,   decelerationFactor);
        float pitchAccel = computeAcceleration(diff.deltaPitch(), prevDiff.deltaPitch(), pitchAccelerationMin, pitchAccelerationMax, decelerationFactor);

        float newDeltaYaw   = prevDiff.deltaYaw()   + yawAccel   + getError(yawAccel,   yawAccelError,   yawConstantError);
        float newDeltaPitch = prevDiff.deltaPitch() + pitchAccel + getError(pitchAccel, pitchAccelError, pitchConstantError);

        return new Rotation(
            currentRotation.yaw()   + newDeltaYaw,
            Mth.clamp(currentRotation.pitch() + newDeltaPitch, -90f, 90f)
        );
    }

    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
        if (Mth.equal(diff.deltaYaw(), 0f) && Mth.equal(diff.deltaPitch(), 0f)) return 0;

        float newYawDiff   = computeAcceleration(diff.deltaYaw(),   0f, yawAccelerationMin,   yawAccelerationMax,   1f);
        float newPitchDiff = computeAcceleration(diff.deltaPitch(), 0f, pitchAccelerationMin, pitchAccelerationMax, 1f);

        if (Mth.equal(newYawDiff, 0f) && Mth.equal(newPitchDiff, 0f)) return 0;

        float ticksH = (float) Math.floor(Math.abs(diff.deltaYaw())   / Math.max(Math.abs(newYawDiff),   0.001f));
        float ticksV = (float) Math.floor(Math.abs(diff.deltaPitch()) / Math.max(Math.abs(newPitchDiff), 0.001f));

        return (int) Math.max(ticksH, ticksV);
    }

    private float computeAcceleration(float diff, float prevDiff, float accelMin, float accelMax, float decelerationFactor) {
        float angleDiff = Rotation.angleDifference(diff, prevDiff);
        float accel = random(-accelMin, accelMax);
        return Mth.clamp(angleDiff, -Math.abs(accel), Math.abs(accel)) * decelerationFactor;
    }

    private float computeSigmoidDeceleration(float rotationDifference) {
        float scaled = rotationDifference / 120f;
        return (float) (1.0 / (1.0 + Math.exp(-sigmoidSteepness * (scaled - sigmoidMidpoint))));
    }

    private float getError(float acceleration, float accelError, float constantError) {
        return acceleration * random(-accelError, accelError) + random(-constantError, constantError);
    }

    private static float random(float min, float max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
}
