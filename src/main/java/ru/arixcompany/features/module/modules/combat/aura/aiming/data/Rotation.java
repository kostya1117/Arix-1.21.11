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

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.back.EaseInOutBack;
import ru.arixcompany.utils.animation.impl.bounce.EaseInBounce;
import ru.arixcompany.utils.animation.impl.bounce.EaseOutBounce;
import ru.arixcompany.utils.animation.impl.circ.EaseInOutCirc;
import ru.arixcompany.utils.animation.impl.cubic.EaseInOutCubic;
import ru.arixcompany.utils.animation.impl.quad.EaseInOutQuad;
import ru.arixcompany.utils.animation.impl.quad.EaseInQuad;
import ru.arixcompany.utils.animation.impl.quad.EaseOutQuad;
import ru.arixcompany.utils.animation.impl.quint.EaseInQuint;
import ru.arixcompany.utils.animation.impl.sine.EaseOutSine;
import ru.arixcompany.utils.math.Interpolate;
import ru.arixcompany.utils.math.MathUtils;

import static ru.arixcompany.utils.math.Interpolate.PROFILES;

public record Rotation(float yaw, float pitch, boolean isNormalized) implements IMinecraft {

    public static final Rotation ZERO = new Rotation(0f, 0f);

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotation(Entity entity) {
        this(entity.getYRot(), entity.getXRot(), false);
    }

    /**
     * @return Vec3 direction this rotation is pointing at
     */
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
        if (this.isNormalized) {
            return this;
        }

        double gcd = getGcd();

        Rotation currentRotation = RotationManager.currentRotation != null
                ? RotationManager.currentRotation
                : new Rotation(mc.player.getYRot(), mc.player.getXRot(),true);

        var diff = currentRotation.rotationDeltaTo(this);

        double g1 = Math.round(diff.deltaYaw() / gcd) * gcd;
        double g2 = Math.round(diff.deltaPitch() / gcd) * gcd;

        float yaw = currentRotation.yaw + (float) g1;
        float pitch = currentRotation.pitch + (float) g2;

        float clampedPitch = Math.clamp(pitch, -90f, 90f);

        return new Rotation(yaw, clampedPitch, true);
    }


    public static double getGcd() {
        double f = mc.options.sensitivity().get() * 0.6D + 0.2D;
        return f * f * f * 8.0D * 0.15D;
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
     * <p>
     * Wrapped 360° for yaw, clamped for pitch
     */
    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(
                angleDifference(other.yaw, this.yaw),
                angleDifference(other.pitch, this.pitch)
        );
    }
public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
    RotationDelta diff = rotationDeltaTo(other);

    float rotationDifference = diff.length();

    float maxYaw   = Math.abs(diff.deltaYaw()   / rotationDifference) * horizontalFactor;
    float maxPitch = Math.abs(diff.deltaPitch()  / rotationDifference) * verticalFactor;

    Interpolate.StepProfile profile = Interpolate.PROFILES.getOrDefault(
            HitAura.interrot.getSelected(),
            Interpolate.PROFILES.get("Линейное")
    );

    float yawStep   = Math.copySign(Interpolate.applyStep(diff.deltaYaw(),   maxYaw,   profile), diff.deltaYaw());
    float pitchStep = Math.copySign(Interpolate.applyStep(diff.deltaPitch(),  maxPitch, profile), diff.deltaPitch());

    return new Rotation(this.yaw + yawStep, this.pitch + pitchStep);
}


    /**
     * Interpolates toward other rotation using sensitivity-based factor.
     * Mimics real mouse movement speed. Uses Mth.rotLerp for proper yaw wrapping.
     * 8.0F * 0.15F = 1.2F makes the speed exactly proportional to Minecraft's GCD.
     */
    public Rotation interpolateToSmooth(Rotation other, float partialTick) {
        double f = mc.options.sensitivity().get() * 0.6D + 0.2F;
        float factor = (float) (f * f * f * 8.0F * 0.15F) * partialTick;

        double d2 = Minecraft.getInstance().options.sensitivity().get() * 0.6F + 0.2F;
        float interpolationFactor = (float) (Math.pow(d2, 3) * 8.0F * 0.15F * partialTick);

        return new Rotation(
                Mth.rotLerp(interpolationFactor, this.yaw, other.yaw),
                Mth.lerp(interpolationFactor, this.pitch, other.pitch)
        );
    }

    public Rotation towardsLinearElytra(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = rotationDeltaTo(other);

        float targetYaw = this.yaw + clamp(diff.deltaYaw(), -horizontalFactor, horizontalFactor);
        float targetPitch = this.pitch + clamp(diff.deltaPitch(), -verticalFactor, verticalFactor);

        return new Rotation(targetYaw, targetPitch);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Interpolates this rotation towards [other] using the given [factor].
     */
    public Rotation interpolateTo(Rotation other, float factor) {
        return new Rotation(
                fmaLerp(factor, yaw,   other.yaw),
                fmaLerp(factor, pitch, other.pitch));
    }
    public Rotation interpolateToNoYaw(Rotation other, float factor) {
        return new Rotation(other.yaw, fmaLerp(factor, pitch, other.pitch));
    }

    public static float fmaLerp(float factor, float from, float to) {
        return Math.fma(factor, to - from, from);
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        return angleTo(other) <= tolerance;
    }

    public boolean approximatelyEquals(Rotation other) {
        return approximatelyEquals(other, 2f);
    }

    public Rotation add(float x, float y) {
        return new Rotation(this.yaw + x, this.pitch + y);
    }

    public static float angleDifference(float a, float b) {
        return Mth.wrapDegrees(a - b);
    }
}
