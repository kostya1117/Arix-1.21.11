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

import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;

import java.util.concurrent.ThreadLocalRandom;

public class LinearAngleSmooth extends FactorAngleSmooth {

    private final float horizontalTurnSpeedMin;
    private final float horizontalTurnSpeedMax;
    private final float verticalTurnSpeedMin;
    private final float verticalTurnSpeedMax;

    public LinearAngleSmooth(float horizontalTurnSpeedMin, float horizontalTurnSpeedMax,
                             float verticalTurnSpeedMin, float verticalTurnSpeedMax) {
        this.horizontalTurnSpeedMin = horizontalTurnSpeedMin;
        this.horizontalTurnSpeedMax = horizontalTurnSpeedMax;
        this.verticalTurnSpeedMin   = verticalTurnSpeedMin;
        this.verticalTurnSpeedMax   = verticalTurnSpeedMax;
    }

    @Override
    public float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (rotationTarget != null) {
            return new float[]{
                random(horizontalTurnSpeedMin, horizontalTurnSpeedMax),
                random(verticalTurnSpeedMin,   verticalTurnSpeedMax)
            };
        } else {
            // Slowest turn speed, so we can calculate the slowest turn speed
            return new float[]{ horizontalTurnSpeedMin, verticalTurnSpeedMin };
        }
    }

    private static float random(float min, float max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
}
