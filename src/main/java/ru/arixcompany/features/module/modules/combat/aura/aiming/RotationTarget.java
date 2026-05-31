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
package ru.arixcompany.features.module.modules.combat.aura.aiming;

import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.MovementCorrection;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.RotationProcessor;
import ru.arixcompany.utils.IMinecraft;

import java.util.List;

/**
 * An aim plan is a plan to aim at a certain rotation.
 * It is being used to calculate the next rotation to aim at.
 *
 * @param rotation The rotation we want to aim at.
 * @param angleSmooth The mode of the smoother.
 */
public class RotationTarget implements IMinecraft {

    public final Rotation rotation;

    /**
     * The rotation processors which are being used to calculate the next rotation.
     * This list should start with [AngleSmooth]
     * and then continue with other processors like [ShortStopRotationProcessor] and [FailFocus].
     */
    public final List<RotationProcessor> processors;

    /**
     * The ticks until reset defines the amount of ticks until we are rotating back.
     */
    public final int ticksUntilReset;

    /**
     * The reset threshold defines the threshold at which we are going to reset the aim plan.
     * The threshold is being calculated by the distance between the current rotation and the rotation we want
     * to aim.
     */
    public final float resetThreshold;

    /**
     * Movement correction mode for this rotation target.
     * Mirrors LiquidBounce's MovementCorrection.
     */
    public final MovementCorrection movementCorrection;

    public RotationTarget(Rotation rotation,
                          List<RotationProcessor> processors,
                          int ticksUntilReset,
                          float resetThreshold) {
        this(rotation, processors, ticksUntilReset, resetThreshold, MovementCorrection.SILENT);
    }

    public RotationTarget(Rotation rotation,
                          List<RotationProcessor> processors,
                          int ticksUntilReset,
                          float resetThreshold,
                          MovementCorrection movementCorrection) {
        this.rotation           = rotation;
        this.processors         = processors;
        this.ticksUntilReset    = ticksUntilReset;
        this.resetThreshold     = resetThreshold;
        this.movementCorrection = movementCorrection;
    }

    /**
     * Calculates the next rotation to aim at.
     * [currentRotation] is the current rotation or rather last rotation we aimed at. It is being used to
     * calculate the next rotation.
     *
     * We might even return null if we do not want to aim at anything yet.
     */
    public Rotation towards(Rotation currentRotation, boolean isResetting) {
        if (isResetting) {
            return process(currentRotation, new Rotation(mc.gameRenderer.getMainCamera().yRot(),mc.gameRenderer.getMainCamera().xRot()));
        }
        return process(currentRotation, rotation);
    }

    private Rotation process(Rotation currentRotation, Rotation targetRotation) {
        if (processors.isEmpty()) return targetRotation;
        Rotation result = targetRotation;
        for (RotationProcessor p : processors) {
            result = p.process(this, currentRotation, result);
        }
        return result;
    }
}
