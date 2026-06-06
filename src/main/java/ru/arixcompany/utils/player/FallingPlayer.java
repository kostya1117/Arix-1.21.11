// FallingPlayer.java
package ru.arixcompany.utils.player;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

@SuppressWarnings("LongParameterList")
public class FallingPlayer {

    private final LocalPlayer player;
    public double x;
    public double y;
    public double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private final float yRot;
    private int simulatedTicks = 0;
    @Getter
    private double predictedFallDistance; // добавлено

    public FallingPlayer(LocalPlayer player, double x, double y, double z,
                         double motionX, double motionY, double motionZ, float yRot) {
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yRot = yRot;
        this.predictedFallDistance = player.fallDistance; // инициализация текущим значением
    }

    public static FallingPlayer fromPlayer(LocalPlayer player) {
        return new FallingPlayer(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getDeltaMovement().x,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z,
                player.getYRot()
        );
    }

    private void calculateForTick(Vec3 rotationVec) {
        double prevY = this.y; // сохраняем предыдущую позицию для расчета fallDistance

        double d = 0.08;
        boolean bl = this.motionY <= 0.0;

        if (bl && hasStatusEffect(MobEffects.SLOW_FALLING)) {
            d = 0.01;
        }

        double j = (double) this.player.getXRot() * Mth.DEG_TO_RAD;

        double k = Math.sqrt(rotationVec.x * rotationVec.x + rotationVec.z * rotationVec.z);
        double l = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);

        double m = rotationVec.length();
        double n = Mth.cos((float) j);

        n = (float) (n * n * Math.min(1.0, m / 0.4));

        Vec3 vec3d5 = new Vec3(this.motionX, this.motionY, this.motionZ)
                .add(0.0, d * (-1.0 + n * 0.75), 0.0);

        double q;
        if (vec3d5.y < 0.0 && k > 0.0) {
            q = vec3d5.y * -0.1 * n;
            vec3d5 = vec3d5.add(rotationVec.x * q / k, q, rotationVec.z * q / k);
        }

        if (j < 0.0 && k > 0.0) {
            q = l * (-Mth.sin((float) j)) * 0.04;
            vec3d5 = vec3d5.add(-rotationVec.x * q / k, q * 3.2, -rotationVec.z * q / k);
        }

        if (k > 0.0) {
            vec3d5 = vec3d5.add((rotationVec.x / k * l - vec3d5.x) * 0.1, 0.0, (rotationVec.z / k * l - vec3d5.z) * 0.1);
        }

        vec3d5 = vec3d5.add(
                Entity.getInputVector(
                        new Vec3(
                                this.player.input.leftImpulse * 0.98,
                                0.0,
                                this.player.input.forwardImpulse * 0.98
                        ),
                        0.02F,
                        yRot
                )
        );

        float velocityCoFactor = this.player.getBlockSpeedFactor();

        this.motionX = vec3d5.x * 0.9900000095367432 * velocityCoFactor;
        this.motionY = vec3d5.y * 0.9800000190734863;
        this.motionZ = vec3d5.z * 0.9900000095367432 * velocityCoFactor;

        this.x += this.motionX;
        this.y += this.motionY;
        this.z += this.motionZ;

        // Обновляем предиктенный fallDistance
        if (this.motionY < 0) {
            double fallDistance = prevY - this.y;
            this.predictedFallDistance += (float) fallDistance;
        } else {
            this.predictedFallDistance = 0.0f;
        }

        this.simulatedTicks++;
    }

    private boolean hasStatusEffect(Holder<MobEffect> effect) {
        var instance = player.getEffect(effect);
        if (instance == null) return false;

        return instance.getDuration() >= this.simulatedTicks;
    }

    public CollisionResult findCollision(float ticks) {
        Vec3 rotationVec = player.getLookAngle();

        for (float i = 0; i < ticks; i++) {
            Vec3 start = new Vec3(x, y, z);

            calculateForTick(rotationVec);

            Vec3 end = new Vec3(x, y, z);

            AABB box = player.getDimensions(Pose.STANDING)
                    .makeBoundingBox(start.x, start.y, start.z)
                    .expandTowards(end.subtract(start));

            Optional<BlockPos> supportBlock = player.level().findSupportingBlock(player, box);

            if (supportBlock.isPresent()) {
                return new CollisionResult(supportBlock.get(), i, this.predictedFallDistance);
            }
        }
        return null;
    }

    @Getter
    public static class CollisionResult {
        private final BlockPos pos;
        private final float tick;
        private final double predictedFallDistance;

        public CollisionResult(BlockPos pos, float tick, double predictedFallDistance) {
            this.pos = pos;
            this.tick = tick;
            this.predictedFallDistance = predictedFallDistance;
        }
    }
}