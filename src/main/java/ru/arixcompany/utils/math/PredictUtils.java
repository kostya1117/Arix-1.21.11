package ru.arixcompany.utils.math;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PredictUtils {

    /**
     * Предикция позиции игрока на элитре через N тиков.
     * Симулирует физику элитры пошагово (точная копия из LivingEntity.updateFallFlyingMovement).
     */
    public static Vec3 predict(LivingEntity entity, Vec3 pos, float ticks, float tps) {
        double hSpeed = Math.hypot(entity.getX() - entity.xOld, entity.getZ() - entity.zOld) * tps;
        double vSpeed = Math.abs(entity.getY() - entity.yOld) * tps;
        if (hSpeed <= 5) {
            return pos;
        }

        Vec3 currentPos = pos;
        Vec3 currentVel = entity.getDeltaMovement();

        for (int t = 0; t < ticks; t++) {
            currentVel = simulateElytraTick(entity, currentVel);
            currentPos = currentPos.add(currentVel);
        }

        return currentPos;
    }

    /**
     * Один тик физики элитры — точная копия логики из LivingEntity.updateFallFlyingMovement().
     */
    private static Vec3 simulateElytraTick(LivingEntity entity, Vec3 velocity) {
        // getLookAngle() из entity
        float pitch = entity.getXRot() * (float) (Math.PI / 180.0);
        float yaw = entity.getYRot() * (float) (Math.PI / 180.0);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        float sinYaw = Mth.sin(yaw);
        float cosYaw = Mth.cos(yaw);
        Vec3 lookVec = new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);

        double lookHLen = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double hVelLen = velocity.horizontalDistance();

        // getEffectiveGravity()
        boolean slowFalling = velocity.y <= 0.0 && entity.hasEffect(MobEffects.SLOW_FALLING);
        double gravity = slowFalling
                ? Math.min(entity.getGravity(), 0.01)
                : entity.getGravity();

        double cosPitch2 = Mth.square(Math.cos(pitch));

        // точная копия из updateFallFlyingMovement (строки 2439-2456 твоего файла)
        velocity = velocity.add(0.0, gravity * (-1.0 + cosPitch2 * 0.75), 0.0);

        if (velocity.y < 0.0 && lookHLen > 0.0) {
            double d4 = velocity.y * -0.1 * cosPitch2;
            velocity = velocity.add(lookVec.x * d4 / lookHLen, d4, lookVec.z * d4 / lookHLen);
        }

        if (pitch < 0.0F && lookHLen > 0.0) {
            double d5 = hVelLen * -Mth.sin(pitch) * 0.04;
            velocity = velocity.add(-lookVec.x * d5 / lookHLen, d5 * 3.2, -lookVec.z * d5 / lookHLen);
        }

        if (lookHLen > 0.0) {
            velocity = velocity.add((lookVec.x / lookHLen * hVelLen - velocity.x) * 0.1, 0.0, (lookVec.z / lookHLen * hVelLen - velocity.z) * 0.1);
        }

        return velocity.multiply(0.99F, 0.98F, 0.99F);
    }
}
