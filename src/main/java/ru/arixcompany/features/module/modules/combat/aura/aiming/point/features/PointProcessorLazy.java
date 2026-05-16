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
public class PointProcessorLazy extends PointProcessor {

    public float thresholdMin = 0.1f;
    public float thresholdMax = 0.2f;

    private float currentThreshold;
    private PointInsideBox currentPoint = null;

    public PointProcessorLazy() {
        currentThreshold = randomThreshold();
    }

    @Override
    public PointInsideBox process(PointInsideBox point) {
        if (currentPoint == null) {
            currentPoint = point;
            return point;
        }

        double distSqr = point.distanceToSqr(currentPoint);
        double thresholdSqr = currentThreshold * currentThreshold;

        // Check if the current point has not reached the minimum threshold to move
        if (distSqr < thresholdSqr) {
            return currentPoint;
        }

        currentPoint     = point;
        currentThreshold = randomThreshold();
        return currentPoint;
    }

    private float randomThreshold() {
        return thresholdMin + ThreadLocalRandom.current().nextFloat() * (thresholdMax - thresholdMin);
    }
}
