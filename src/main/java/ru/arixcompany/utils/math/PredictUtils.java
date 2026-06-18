package ru.arixcompany.utils.math;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.utils.IMinecraft;

public class PredictUtils implements IMinecraft {

    public static Vec3 predict(LivingEntity target, int ticks) {
        if (ticks <= 0) {
            return target.position();
        }

        if (!target.isFallFlying()) {
            return target.getEyePosition(1.0F);
        }

        Vec3 pos = target.position();
        Vec3 vel = target.getDeltaMovement().scale(mc.level.tickRateManager().tickrate()); // мб че та

        for (int i = 0; i < ticks; i++) {
            vel = target.updateFallFlyingMovement(vel);
            pos = pos.add(vel);
        }

        return pos;
    }
}