package ru.arixcompany.features.module.modules.combat.aura.rotation;

import lombok.Getter;
import net.minecraft.util.Mth;

@Getter
public class Rotation {
    private final float yaw;
    private final float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = Mth.wrapDegrees(yaw);
        this.pitch = Mth.clamp(pitch, -90F, 90F);
    }

    public float getDelta(Rotation other) {
        float yawDelta = Mth.wrapDegrees(other.yaw - this.yaw);
        float pitchDelta = Mth.wrapDegrees(other.pitch - this.pitch);
        return (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
}