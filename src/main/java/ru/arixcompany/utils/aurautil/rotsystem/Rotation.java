package ru.arixcompany.utils.aurautil.rotsystem;

import net.minecraft.util.Mth;

public class Rotation {
    private final float yaw;
    private final float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = Mth.wrapDegrees(yaw);
        this.pitch = Mth.clamp(pitch, -90F, 90F);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getDelta(Rotation other) {
        float yawDelta = Mth.wrapDegrees(other.yaw - this.yaw);
        float pitchDelta = Mth.wrapDegrees(other.pitch - this.pitch);
        return (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
}