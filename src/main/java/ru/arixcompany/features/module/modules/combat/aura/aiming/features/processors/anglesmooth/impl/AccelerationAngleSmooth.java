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
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.RotationDelta;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.AngleSmooth;
import ru.arixcompany.utils.IMinecraft;

import java.util.concurrent.ThreadLocalRandom;

//public class AccelerationAngleSmooth extends AngleSmooth {
//
//    private final float yawAccelerationMin;
//    private final float yawAccelerationMax;
//    private final float pitchAccelerationMin;
//    private final float pitchAccelerationMax;
//
//    private final boolean sigmoidEnabled;
//    private final float sigmoidSteepness;
//    private final float sigmoidMidpoint;
//
//    private final float yawAccelError;
//    private final float pitchAccelError;
//    private final float yawConstantError;
//    private final float pitchConstantError;
//
//    public AccelerationAngleSmooth(float yawAccelerationMin, float yawAccelerationMax,
//                                   float pitchAccelerationMin, float pitchAccelerationMax,
//                                   boolean sigmoidEnabled, float sigmoidSteepness, float sigmoidMidpoint,
//                                   float yawAccelError, float pitchAccelError,
//                                   float yawConstantError, float pitchConstantError) {
//        this.yawAccelerationMin   = yawAccelerationMin;
//        this.yawAccelerationMax   = yawAccelerationMax;
//        this.pitchAccelerationMin = pitchAccelerationMin;
//        this.pitchAccelerationMax = pitchAccelerationMax;
//        this.sigmoidEnabled       = sigmoidEnabled;
//        this.sigmoidSteepness     = sigmoidSteepness;
//        this.sigmoidMidpoint      = sigmoidMidpoint;
//        this.yawAccelError        = yawAccelError;
//        this.pitchAccelError      = pitchAccelError;
//        this.yawConstantError     = yawConstantError;
//        this.pitchConstantError   = pitchConstantError;
//    }
//
//    public AccelerationAngleSmooth() {
//        this(20f, 25f, 20f, 25f, false, 10f, 0.3f, 0.1f, 0.1f, 0.1f, 0.1f);
//    }
//
//    @Override
//    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
//        Rotation prevRotation = RotationManager.previousRotation != null
//                ? RotationManager.previousRotation
//                : currentRotation;
//
//        RotationDelta prevDiff = prevRotation.rotationDeltaTo(currentRotation);
//        RotationDelta diff     = currentRotation.rotationDeltaTo(targetRotation);
//
//        float decelerationFactor = sigmoidEnabled
//                ? computeSigmoidDeceleration(diff.length())
//                : 1.0f;
//
//        float yawAccel   = computeAcceleration(diff.deltaYaw(),   prevDiff.deltaYaw(),   yawAccelerationMin,   yawAccelerationMax,   decelerationFactor);
//        float pitchAccel = computeAcceleration(diff.deltaPitch(), prevDiff.deltaPitch(), pitchAccelerationMin, pitchAccelerationMax, decelerationFactor);
//
//        float newDeltaYaw   = prevDiff.deltaYaw()   + yawAccel   + getError(yawAccel,   yawAccelError,   yawConstantError);
//        float newDeltaPitch = prevDiff.deltaPitch() + pitchAccel + getError(pitchAccel, pitchAccelError, pitchConstantError);
//
//        return new Rotation(
//            currentRotation.yaw()   + newDeltaYaw,
//            Mth.clamp(currentRotation.pitch() + newDeltaPitch, -90f, 90f)
//        );
//    }
//
//    @Override
//    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
//        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
//        if (Mth.equal(diff.deltaYaw(), 0f) && Mth.equal(diff.deltaPitch(), 0f)) return 0;
//
//        float newYawDiff   = computeAcceleration(diff.deltaYaw(),   0f, yawAccelerationMin,   yawAccelerationMax,   1f);
//        float newPitchDiff = computeAcceleration(diff.deltaPitch(), 0f, pitchAccelerationMin, pitchAccelerationMax, 1f);
//
//        if (Mth.equal(newYawDiff, 0f) && Mth.equal(newPitchDiff, 0f)) return 0;
//
//        float ticksH = (float) Math.floor(Math.abs(diff.deltaYaw())   / Math.max(Math.abs(newYawDiff),   0.001f));
//        float ticksV = (float) Math.floor(Math.abs(diff.deltaPitch()) / Math.max(Math.abs(newPitchDiff), 0.001f));
//
//        return (int) Math.max(ticksH, ticksV);
//    }
//
//    private float computeAcceleration(float diff, float prevDiff, float accelMin, float accelMax, float decelerationFactor) {
//        float angleDiff = Rotation.angleDifference(diff, prevDiff);
//        float accel = random(-accelMin, accelMax);
//        return Mth.clamp(angleDiff, -Math.abs(accel), Math.abs(accel)) * decelerationFactor;
//    }
//
//    private float computeSigmoidDeceleration(float rotationDifference) {
//        // Нормализуем угол к диапазону [0, 1] где 180° = 1.0
//        float scaled = Math.min(rotationDifference / 180f, 1f);
//        // Применяем sigmoid функцию для плавного замедления
//        return (float) (1.0 / (1.0 + Math.exp(-sigmoidSteepness * (scaled - sigmoidMidpoint))));
//    }
//
//    private float getError(float acceleration, float accelError, float constantError) {
//        return acceleration * random(-accelError, accelError) + random(-constantError, constantError);
//    }
//
//    private static float random(float min, float max) {
//        if (min >= max) return min;
//        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
//    }
//}
public class AccelerationAngleSmooth extends AngleSmooth implements IMinecraft {

    // Основные настройки
    private float yawAccelerationMin = 6f, yawAccelerationMax = 11f;
    private float pitchAccelerationMin = 6f, pitchAccelerationMax = 11f;

    // Динамическое ускорение (DynamicAccel)
    private boolean dynamicEnabled = false;
    private float coefDistance = -1.393f;
    private float yawCrosshairAccelMin = 3f, yawCrosshairAccelMax = 7f;
    private float pitchCrosshairAccelMin = 3f, pitchCrosshairAccelMax = 7f;

    // Сигмоида замедления
    private boolean sigmoidEnabled = false;
    private float sigmoidSteepness = 10f;
    private float sigmoidMidpoint = 0.3f;

    // Ошибки
    private float yawAccelError = 0.1f, pitchAccelError = 0.1f;
    private float yawConstantError = 0.1f, pitchConstantError = 0.1f;

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        // ВАЖНО: берем lastRotation игрока, если нет предыдущей в менеджере
        Rotation prevRotation = RotationManager.previousRotation != null
                ? RotationManager.previousRotation
                : new Rotation(mc.player.yRotO, mc.player.xRotO);

        RotationDelta prevDiff = prevRotation.rotationDeltaTo(currentRotation);
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);

        // Расчет дистанции и проверки наведения (как в оригинале)
        double distance = mc.player.distanceTo(HitAura.target);
        boolean crosshair = AttackHandler.anyEntityOnRay(currentRotation,HitAura.target, (float) distance);

        float decelerationFactor = sigmoidEnabled ? computeSigmoidDeceleration(diff.length()) : 1.0f;

        // Логика Dynamic Acceleration
        boolean useCrosshair = dynamicEnabled && crosshair;
        float distFactor = (float) (coefDistance * distance);

        float aYawMin = useCrosshair ? yawCrosshairAccelMin : yawAccelerationMin;
        float aYawMax = useCrosshair ? yawCrosshairAccelMax : yawAccelerationMax;
        float aPitchMin = useCrosshair ? pitchCrosshairAccelMin : pitchAccelerationMin;
        float aPitchMax = useCrosshair ? pitchCrosshairAccelMax : pitchAccelerationMax;

        float yawAccel = calculateAcceleration(diff.deltaYaw(), prevDiff.deltaYaw(),
                -random(aYawMin, aYawMax) + distFactor, random(aYawMin, aYawMax) + distFactor, decelerationFactor);

        float pitchAccel = calculateAcceleration(diff.deltaPitch(), prevDiff.deltaPitch(),
                -random(aPitchMin, aPitchMax) + distFactor, random(aPitchMin, aPitchMax) + distFactor, decelerationFactor);

        // Добавляем ошибки (ErrorProvider)
        float finalYawDelta = prevDiff.deltaYaw() + yawAccel + getError(yawAccel, yawAccelError, yawConstantError);
        float finalPitchDelta = prevDiff.deltaPitch() + pitchAccel + getError(pitchAccel, pitchAccelError, pitchConstantError);

        return new Rotation(
                currentRotation.yaw() + finalYawDelta,
                Mth.clamp(currentRotation.pitch() + finalPitchDelta, -90f, 90f)
        );
    }
    @Override
    public int calculateTicks(Rotation currentRotation, Rotation targetRotation) {
        RotationDelta diff = currentRotation.rotationDeltaTo(targetRotation);
        if (Mth.equal(diff.deltaYaw(), 0f) && Mth.equal(diff.deltaPitch(), 0f)) return 0;

        float newYawDiff   = calculateAcceleration(diff.deltaYaw(),   0f, yawAccelerationMin,   yawAccelerationMax,   1f);
        float newPitchDiff = calculateAcceleration(diff.deltaPitch(), 0f, pitchAccelerationMin, pitchAccelerationMax, 1f);

        if (Mth.equal(newYawDiff, 0f) && Mth.equal(newPitchDiff, 0f)) return 0;

        var ticksH = Math.floor(Math.abs(diff.deltaYaw()) / Math.abs(newYawDiff));
        var ticksV = Math.floor(Math.abs(diff.deltaPitch()) / Math.abs(newPitchDiff));

        return (int) Math.max(ticksH, ticksV);
    }

    private float calculateAcceleration(float diff, float prevDiff, float min, float max, float factor) {
        float angleDiff = Rotation.angleDifference(diff, prevDiff);
        return Mth.clamp(angleDiff, min, max) * factor;
    }

    private float computeSigmoidDeceleration(float rotationDifference) {
        // В оригинале делится на 120.0
        float scaled = rotationDifference / 120f;
        return (float) (1.0 / (1.0 + Math.exp(-sigmoidSteepness * (scaled - sigmoidMidpoint))));
    }

    private float getError(float acceleration, float accelError, float constantError) {
        // В оригинале генерируется один раз для Yaw и один раз для Pitch
        return acceleration * random(-accelError, accelError) + random(-constantError, constantError);
    }

    private static float random(float min, float max) {
        return min >= max ? min : min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
}
