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
package ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors;

import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Short stop temporarily halts aiming at the target based on a specified rate.
 */
public class ShortStopRotationProcessor implements RotationProcessor {

    private final int rate;
    private final int durationMin;
    private final int durationMax;

    private int ticksElapsed = 0;
    private int currentTransitionInDuration;

    public ShortStopRotationProcessor(int rate, int durationMin, int durationMax) {
        this.rate = rate;
        this.durationMin = durationMin;
        this.durationMax = durationMax;
        this.currentTransitionInDuration = randomInt(durationMin, durationMax);
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (rate > ThreadLocalRandom.current().nextInt(101)) {
            currentTransitionInDuration = randomInt(durationMin, durationMax);
            ticksElapsed = 0;
        }

        if (ticksElapsed < currentTransitionInDuration) {
            ticksElapsed++;
            return currentRotation.towardsLinear(targetRotation,
                ThreadLocalRandom.current().nextFloat() * 0.1f,
                ThreadLocalRandom.current().nextFloat() * 0.1f);
        }

        return targetRotation;
    }

    private static int randomInt(int min, int max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }
}
