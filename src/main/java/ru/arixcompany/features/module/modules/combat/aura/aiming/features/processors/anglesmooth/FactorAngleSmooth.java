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
package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.RotationProcessor;

/**
 * horizontal speed, vertical speed
 */
public abstract class FactorAngleSmooth implements RotationProcessor {

    /**
     * Calculate the factors for the rotation towards the target rotation.
     *
     * @param currentRotation The current rotation
     * @param targetRotation  The target rotation
     * @return horizontal speed, vertical speed as float[2]
     */
    public abstract float[] calculateFactors(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation);

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        float[] factors = calculateFactors(rotationTarget, currentRotation, targetRotation);
        return currentRotation.towardsLinear(targetRotation, factors[0], factors[1]);
    }
}
