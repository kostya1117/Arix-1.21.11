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

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.utils.IMinecraft;

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
        if (isNormalized) return this;
        if (mc.player == null) return ZERO;

        double sens = mc.options.sensitivity().get() * 0.6 + 0.2;
        double gcd = sens * sens * sens * 8.0 * 0.15;

        Rotation currentRotation = RotationManager.currentRotation != null
                ? RotationManager.currentRotation
                : new Rotation(mc.player);

        RotationDelta diff = currentRotation.rotationDeltaTo(this);

        float g1 = (float) (Math.round(diff.deltaYaw() / gcd) * gcd);
        float g2 = (float) (Math.round(diff.deltaPitch() / gcd) * gcd);

        float newYaw   = currentRotation.yaw + g1;
        float newPitch = Mth.clamp(currentRotation.pitch + g2, -90f, 90f);

        return new Rotation(newYaw, newPitch, true);
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
     * Wrapped 360°
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
    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = rotationDeltaTo(other);
        float rotationDifference = diff.length();
        if (rotationDifference == 0f) return this;

        float straightLineYaw   = Math.abs(diff.deltaYaw()   / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.deltaPitch() / rotationDifference) * verticalFactor;

        return new Rotation(
            this.yaw   + Mth.clamp(diff.deltaYaw(),   -straightLineYaw,   straightLineYaw),
            this.pitch + Mth.clamp(diff.deltaPitch(), -straightLinePitch, straightLinePitch)
        );
    }
    public static float crosshairAngleToEntity(Entity entity) {
        if (mc.player == null) {
            return 0.0F;
        }

        Vec3 eyes = mc.player.getEyePosition(1.0F);
        Vec3 targetPoint = entity.getBoundingBox().getCenter();

        Rotation rotationToEntity = Rotation.lookingAt(targetPoint, eyes);
        Rotation playerRotation = new Rotation(mc.player);

        return playerRotation.angleTo(rotationToEntity);
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
