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

import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;

import java.util.concurrent.ThreadLocalRandom;

public class InterpolationAngleSmooth extends FactorAngleSmooth {

    private final int horizontalSpeedMin;
    private final int horizontalSpeedMax;
    private final int verticalSpeedMin;
    private final int verticalSpeedMax;
    private final int directionChangeFactorMin;
    private final int directionChangeFactorMax;
    private final float midpoint;

    private Rotation previousTargetRotation = null;

    public InterpolationAngleSmooth(int horizontalSpeedMin, int horizontalSpeedMax,
                                    int verticalSpeedMin, int verticalSpeedMax,
                                    int directionChangeFactorMin, int directionChangeFactorMax,
                                    float midpoint) {
        this.horizontalSpeedMin = horizontalSpeedMin;
        this.horizontalSpeedMax = horizontalSpeedMax;
        this.verticalSpeedMin   = verticalSpeedMin;
        this.verticalSpeedMax   = verticalSpeedMax;
        this.directionChangeFactorMin = directionChangeFactorMin;
        this.directionChangeFactorMax = directionChangeFactorMax;
        this.midpoint = midpoint;
    }

    public InterpolationAngleSmooth() {
        this(80, 85, 20, 25, 95, 100, 0.35f);
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float yawDiff   = Rotation.angleDifference(targetRotation.yaw(),   currentRotation.yaw());
        float pitchDiff = targetRotation.pitch() - currentRotation.pitch();

        float directionChange = 0f;
        if (rotationTarget != null && previousTargetRotation != null) {
            float angleDiff = previousTargetRotation.angleTo(targetRotation);
            float normalized = Math.min(angleDiff / 180f, 1f);
            directionChange = normalized * (randomInt(directionChangeFactorMin, directionChangeFactorMax) / 100f);
        }
        if (rotationTarget != null) previousTargetRotation = targetRotation;

        float hSpeed = (rotationTarget != null ? randomInt(horizontalSpeedMin, horizontalSpeedMax) : horizontalSpeedMin) / 100f;
        float vSpeed = (rotationTarget != null ? randomInt(verticalSpeedMin,   verticalSpeedMax)   : verticalSpeedMin)   / 100f;

        float hFactor = calculateFactor(Math.abs(yawDiff),   hSpeed, directionChange);
        float vFactor = calculateFactor(Math.abs(pitchDiff), vSpeed, directionChange);

        return new float[]{ hFactor * Math.abs(yawDiff), vFactor * Math.abs(pitchDiff) };
    }

    private float calculateFactor(float rotationDifference, float turnSpeed, float directionChange) {
        float t = Math.min(rotationDifference / 180f, 1f);
        if (t > midpoint) {
            return bezierTransform(0.05f, 1f, 1f - t) * turnSpeed;
        } else {
            return sigmoidTransform(t) * Math.min(turnSpeed + directionChange, 1f);
        }
    }

    private static float sigmoidTransform(float t) {
        return (float) (1.0 / (1.0 + Math.exp(-0.5 * (t - 0.3))));
    }

    private static float bezierTransform(float start, float end, float t) {
        return (1f - t) * (1f - t) * start + 2f * (1f - t) * t + t * t * end;
    }

    private static int randomInt(int min, int max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }
}
