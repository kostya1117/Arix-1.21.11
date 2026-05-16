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

import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.PointInsideBox;

import java.security.SecureRandom;

/**
 * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
 * calculate the offset.
 */
public class PointProcessorGaussian extends PointProcessor {

    private static final double STDDEV_Z = 0.24453708645460387;
    private static final double MEAN_X   = 0.00942273861037109;
    private static final double STDDEV_X = 0.23319837528201348;
    private static final double MEAN_Y   = -0.30075078007595923;
    private static final double STDDEV_Y = 0.3492437109081718;
    private static final double MEAN_Z   = 0.013282929419023442;

    private static final SecureRandom random = new SecureRandom();

    public float yawFactorMin   = 0f;
    public float yawFactorMax   = 0f;
    public float pitchFactorMin = 0f;
    public float pitchFactorMax = 0f;
    public int   chance         = 100;
    public float speedMin       = 0.1f;
    public float speedMax       = 0.2f;
    public float tolerance      = 0.05f;

    private Vec3 currentOffset = Vec3.ZERO;
    private Vec3 targetOffset  = Vec3.ZERO;

    @Override
    public PointInsideBox process(PointInsideBox point) {
        float yawFactor   = yawFactorMin   + (float) Math.random() * (yawFactorMax   - yawFactorMin);
        float pitchFactor = pitchFactorMin + (float) Math.random() * (pitchFactorMax - pitchFactorMin);

        if (yawFactor > 0f && pitchFactor > 0f && chance > 0) {
            updateOffset(yawFactor, pitchFactor);
        }

        return point.add(currentOffset);
    }

    private void updateOffset(float yawFactor, float pitchFactor) {
        if (currentOffset.distanceTo(targetOffset) < tolerance) {
            if (random.nextInt(100) <= chance) {
                targetOffset = new Vec3(
                    random.nextGaussian(MEAN_X, STDDEV_X) * yawFactor,
                    random.nextGaussian(MEAN_Y, STDDEV_Y) * pitchFactor,
                    random.nextGaussian(MEAN_Z, STDDEV_Z) * yawFactor
                );
            }
        } else {
            float speed = speedMin + (float) Math.random() * (speedMax - speedMin);
            currentOffset = new Vec3(
                lerp(speed, currentOffset.x, targetOffset.x),
                lerp(speed, currentOffset.y, targetOffset.y),
                lerp(speed, currentOffset.z, targetOffset.z)
            );
        }
    }

    private static double lerp(float t, double a, double b) {
        return a + t * (b - a);
    }
}
