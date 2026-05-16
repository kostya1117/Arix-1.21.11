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
package ru.arixcompany.features.module.modules.combat.aura.aiming.point.features;

import ru.arixcompany.features.module.modules.combat.aura.aiming.point.PointInsideBox;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lazy Point allows you to set a threshold when the point is going to be updated.
 * If the new point is below this threshold, we return the current point instead.
 */
public class PointProcessorDelay extends PointProcessor {

    public int delayMin = 2;
    public int delayMax = 4;

    private int currentDelay;
    private PointInsideBox currentPoint = null;

    public PointProcessorDelay() {
        currentDelay = randomDelay();
    }

    @Override
    public PointInsideBox process(PointInsideBox point) {
        if (point.equals(currentPoint)) return point;

        if (currentPoint == null) {
            currentPoint = point;
            return point;
        }

        // Check if the current delay has not expired yet
        currentDelay--;
        if (currentDelay > 0) {
            return currentPoint;
        }

        currentPoint = point;
        currentDelay = randomDelay();
        return currentPoint;
    }

    private int randomDelay() {
        if (delayMin >= delayMax) return delayMin;
        return delayMin + ThreadLocalRandom.current().nextInt(delayMax - delayMin + 1);
    }
}
