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

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The fail focus acts as fail rate, it will purposely miss the target on a certain rate.
 */
public class FailRotationProcessor extends Component implements RotationProcessor {

    private final int failRate;
    private final float failFactor;
    private final float strengthHorizontalMin;
    private final float strengthHorizontalMax;
    private final float strengthVerticalMin;
    private final float strengthVerticalMax;
    private final int transitionMin;
    private final int transitionMax;

    private int ticksElapsed = 0;
    private int currentTransitionInDuration;
    private float shiftYaw   = 0f;
    private float shiftPitch = 0f;

    public boolean enabled = false;

    public FailRotationProcessor(int failRate, float failFactor,
                                 float strengthHorizontalMin, float strengthHorizontalMax,
                                 float strengthVerticalMin, float strengthVerticalMax,
                                 int transitionMin, int transitionMax) {
        this.failRate = failRate;
        this.failFactor = failFactor;
        this.strengthHorizontalMin = strengthHorizontalMin;
        this.strengthHorizontalMax = strengthHorizontalMax;
        this.strengthVerticalMin = strengthVerticalMin;
        this.strengthVerticalMax = strengthVerticalMax;
        this.transitionMin = transitionMin;
        this.transitionMax = transitionMax;
        this.currentTransitionInDuration = randomInt(transitionMin, transitionMax);
    }

    public FailRotationProcessor() {
        this(3, 0.04f, 5f, 10f, 0f, 2f, 1, 4);
    }

    @EventHandler
    public void onTick(EventGameTick e) {
        float chance = ThreadLocalRandom.current().nextFloat() * 100f;
        if (failRate > chance) {
            currentTransitionInDuration = randomInt(transitionMin, transitionMax);
            shiftYaw   = random(strengthHorizontalMin, strengthHorizontalMax) * (ThreadLocalRandom.current().nextBoolean() ? 1f : -1f);
            shiftPitch = random(strengthVerticalMin,   strengthVerticalMax)   * (ThreadLocalRandom.current().nextBoolean() ? 1f : -1f);
            ticksElapsed = 0;
        } else {
            ticksElapsed++;
        }
    }


    public boolean isInFailState() {
        return enabled && ticksElapsed < currentTransitionInDuration;
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (!isInFailState()) return targetRotation;

        Rotation prevRotation = RotationManager.previousRotation;
        if (prevRotation == null) return targetRotation;

        Rotation serverRotation = RotationManager.actualServerRotation;

        float deltaYaw   = (prevRotation.yaw()   - serverRotation.yaw())   * failFactor;
        float deltaPitch = (prevRotation.pitch() - serverRotation.pitch()) * failFactor;

        return new Rotation(
            targetRotation.yaw()   + deltaYaw   + shiftYaw,
            targetRotation.pitch() + deltaPitch + shiftPitch
        );
    }

    private static float random(float min, float max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    private static int randomInt(int min, int max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }
}
