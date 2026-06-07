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
package ru.arixcompany.features.module.modules.combat.aura.aiming.data;

import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Interpolation;

public record Rotation(float yaw, float pitch, boolean isNormalized) implements IMinecraft {

    public static final Rotation ZERO = new Rotation(0f, 0f);

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotation(Entity entity) {
        this(entity.getYRot(), entity.getXRot(), false);
    }

    /** @return Vec3 direction this rotation is pointing at */
    public Vec3 directionVector() {
        return Vec3.directionFromRotation(pitch, yaw);
    }

    public static Rotation lookingAt(Vec3 point, Vec3 from) {
        return fromRotationVec(point.subtract(from));
    }

    public static Rotation fromRotationVec(Vec3 lookVec) {
        return fromRotationVec(lookVec.x, lookVec.y, lookVec.z);
    }

    public static Rotation fromRotationVec(double diffX, double diffY, double diffZ) {
        return new Rotation(
            Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f)),
            Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ)))))
        );
    }

    /**
     * Fixes GCD and Modulo 360° at yaw
     *
     * @return [Rotation] with fixed yaw and pitch
     */
    public Rotation normalize() {
        if (this.isNormalized) return this;

        var gcd = getGCDValue();

        Rotation currentRotation = RotationManager.currentRotation;
        if (currentRotation == null) {
            currentRotation = RotationManager.playerRotation;
        }

        RotationDelta diff = currentRotation.rotationDeltaTo(this);

        float stepYaw = (float) (Math.round(diff.deltaYaw() / gcd) * gcd);
        float stepPitch = (float) (Math.round(diff.deltaPitch() / gcd) * gcd);

        float yaw = currentRotation.yaw + stepYaw;
        float pitch = Math.clamp(currentRotation.pitch + stepPitch, -90f, 90f);

        return new Rotation(yaw, pitch, true);
    }
    public static double getSensitivity(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public static double getGCDValue() {
        return getGCD() * 0.15;
    }

    public static double getGCD() {
        double d2 = mc.options.sensitivity().get() * 0.6F + 0.2F;
        double d3 = d2 * d2 * d2;
        return d3 * 8.0;
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    /**
     * Calculates the angle between this and the other rotation.
     *
     * @return angle in degrees
     */
    public float angleTo(Rotation other) {
        return Math.min(rotationDeltaTo(other).length(), 180f);
    }

    /**
     * Calculates what angles would need to be added to arrive at [other].
     *
     * Wrapped 360° for yaw, clamped for pitch
     */
    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(
                angleDifference(other.yaw, this.yaw),
                angleDifference(other.pitch, this.pitch)
        );
    }

    /**
     * Calculates a new rotation that is closer to the [other] rotation by a limiting factor of
     * [horizontalFactor] and [verticalFactor], which should be between 0 and 180 degrees.
     */
//    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
//        RotationDelta diff = rotationDeltaTo(other);
//
//        float yawStep = horizontalFactor;
//        float pitchStep = verticalFactor;
//
////        if (Math.abs(diff.deltaYaw()) < yawStep) {
////            yawStep = Math.abs(diff.deltaYaw());
////        }
////        if (Math.abs(diff.deltaPitch()) < pitchStep) {
////            pitchStep = Math.abs(diff.deltaPitch());
////        }
//
//        return new Rotation(
//                this.yaw + Math.copySign(yawStep, diff.deltaYaw()),
//                Mth.clamp(this.pitch + Math.copySign(pitchStep, diff.deltaPitch()), -90f, 90f)
//        );
//    }
    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = rotationDeltaTo(other);

        float rotationDifference = diff.length();

        float straightLineYaw =
                Math.abs(diff.deltaYaw() / rotationDifference) * horizontalFactor;

        float straightLinePitch =
                Math.abs(diff.deltaPitch() / rotationDifference) * verticalFactor;

        return new Rotation(
                this.yaw + clamp(diff.deltaYaw(), -straightLineYaw, straightLineYaw),
                this.pitch + clamp(diff.deltaPitch(), -straightLinePitch, straightLinePitch)
        );
    }
    public Rotation towardsLinearElytra(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = rotationDeltaTo(other);

        float targetYaw = this.yaw + clamp(diff.deltaYaw(), -horizontalFactor, horizontalFactor);
        float targetPitch = this.pitch + clamp(diff.deltaPitch(), -verticalFactor, verticalFactor);

        return new Rotation(targetYaw, targetPitch);
    }

//    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
//        RotationDelta diff = rotationDeltaTo(other);
//
//        float rotationDifference = diff.length();
//
//        // Нормализуем каждую ось отдельно [0..1]
//        float tYaw   = Mth.clamp(Math.abs(diff.deltaYaw())   / 180f, 0f, 1f);
//        float tPitch = Mth.clamp(Math.abs(diff.deltaPitch())  / 90f,  0f, 1f);
//
//        // Кривая для yaw
//        float yawCurve = (float) Interpolation.interpolate(
//                0.0, 1.0, tYaw,
//                Interpolation.Type.BOUNCE,
//                Interpolation.Ease.OUT
//        );
//
//        // Кривая для pitch
//        float pitchCurve = (float) Interpolation.interpolate(
//                0.0, 1.0, tPitch,
//                Interpolation.Type.BOUNCE,
//                Interpolation.Ease.OUT
//        );
//
//        float straightLineYaw   = yawCurve   * horizontalFactor;
//        float straightLinePitch = pitchCurve * verticalFactor;
//
//        float targetYaw   = this.yaw   + clamp(diff.deltaYaw(),   -straightLineYaw,   straightLineYaw);
//        float targetPitch = this.pitch + clamp(diff.deltaPitch(), -straightLinePitch, straightLinePitch);
//
//        // EMA сглаживание с коэффициентом 0.7
//        float partialticks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
//        float f1 = this.mc.level.tickRateManager().isEntityFrozen(mc.player) ? 1.0F : partialticks;
//        float smoothedYaw   = Mth.lerp(f1, targetYaw,   this.yaw);
//        float smoothedPitch = Mth.lerp(f1, targetPitch, this.pitch);
//
//        return new Rotation(smoothedYaw, smoothedPitch);
//    }
//    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
//        RotationDelta diff = rotationDeltaTo(other);
//
//        float targetYaw   = this.yaw   + clamp(diff.deltaYaw(),   -horizontalFactor,   horizontalFactor);
//        float targetPitch = this.pitch + clamp(diff.deltaPitch(), -verticalFactor, verticalFactor);
//
////мда у тебя явные отклонения
////        float smoothedYaw   = Mth.lerp(mc.gameRenderer.getMainCamera().getPartialTickTime(), targetYaw,   this.yaw);
////        float smoothedPitch = Mth.lerp(mc.gameRenderer.getMainCamera().getPartialTickTime(), targetPitch, this.pitch);
//        float smoothedYaw   = Mth.rotLerp(mc.gameRenderer.getMainCamera().getPartialTickTime(), targetYaw, this.yaw);
//        float smoothedPitch   = Mth.rotLerp(mc.gameRenderer.getMainCamera().getPartialTickTime(), targetPitch, this.pitch);
//
//        return new Rotation(smoothedYaw, smoothedPitch);
//    }
//    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
//        RotationDelta diff = rotationDeltaTo(other);
//
//        float targetYaw   = this.yaw   + clamp(diff.deltaYaw(),   -horizontalFactor,   horizontalFactor);
//        float targetPitch = this.pitch + clamp(diff.deltaPitch(), -verticalFactor, verticalFactor);
//
//        return new Rotation(targetYaw, targetPitch);
//    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    /**
     * Interpolates this rotation towards [other] using the given [factor].
     */
    public Rotation interpolateTo(Rotation other, float factor) {
        return new Rotation(
            Math.fma(factor, other.yaw   - yaw,   yaw),
            Math.fma(factor, other.pitch - pitch, pitch)
        );
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        return angleTo(other) <= tolerance;
    }

    public boolean approximatelyEquals(Rotation other) {
        return approximatelyEquals(other, 2f);
    }

    public Rotation add(float x, float y) {
        return new Rotation(this.yaw + y, this.pitch + x);
    }

    public static float angleDifference(float a, float b) {
        return Mth.wrapDegrees(a - b);
    }
}
