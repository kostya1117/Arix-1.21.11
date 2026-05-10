package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

import java.util.concurrent.ThreadLocalRandom;

public final class SnapRotation implements IMinecraft, AbstractRotation {

    private static int tick;

    @Override
    public void rotate(LivingEntity target, boolean attack) {
        if (target == null || mc.player == null) return;

        long now = System.currentTimeMillis();

        float addVacY = 0.25F * (float) Math.cos(now / 1500.0);
        float addVacZ = 0.20F * (float) Math.cos(now / 700.0);
        float addVacX = 0.20F * (float) Math.cos(now / 900.0);

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 vec = new Vec3(target.getX(), target.getY(), target.getZ())
                .add(addVacX,
                     Mth.clamp(eyePos.y - target.getY(), 0.0, 0.8) - addVacY,
                     addVacZ)
                .subtract(eyePos)
                .normalize();

        float targetYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float targetPitch = (float) Mth.clamp(
                -Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))),
                -90.0, 90.0
        );

        switch (HitAura.snapSetting.getSelected()) {

            case "Быстрый" -> {
                float yaw   = attack ? targetYaw   : FreeLookRepo.freeYaw;
                float pitch = attack ? targetPitch : FreeLookRepo.freePitch;
                float speed = MathUtils.randomValue(190.0F, 245.0F);

                RotationRepo.update(
                        new Rotation(yaw, pitch).adjustSensitivity(),
                        speed, speed, 40.0F, 40.0F,
                        1, 7, false
                );
            }

            case "Плавный" -> {
                float speed = 24.0F;

                if (attack) {
                    tick  = 3;
                    speed = 88.0F;
                }

                float yaw   = FreeLookRepo.freeYaw;
                float pitch = FreeLookRepo.freePitch;

                if (tick > 0) {
                    yaw   = targetYaw;
                    pitch = targetPitch;
                    tick--;
                }

                RotationRepo.update(
                        new Rotation(yaw, pitch).adjustSensitivity(),
                        speed, speed, 40.0F, 40.0F,
                        1, 7, false
                );
            }

            case "Рандомный" -> {
                if (attack) {
                    tick = (int) MathUtils.randomValue(2, 4);
                }

                float speed = MathUtils.randomValue(30.0F, 35.0F);
                float yaw   = FreeLookRepo.freeYaw;
                float pitch = FreeLookRepo.freePitch;

                if (tick > 0) {
                    speed = MathUtils.randomValue(140.0F, 220.0F);
                    yaw   = targetYaw;
                    pitch = targetPitch;
                    tick--;
                }

                double t = now;
                float noiseYaw = ThreadLocalRandom.current().nextFloat(-3.0F, 3.0F)
                        + (float) (MathUtils.randomValue(4.0F, 5.0F) * Math.cos(t / 150.0))
                        + (float) (MathUtils.randomValue(4.0F, 5.0F) * Math.sin(t / 50.0))
                        + (float) (MathUtils.randomValue(5.0F, 8.0F) * Math.sin(t / 130.0))
                          * (float) (MathUtils.randomValue(4.0F, 7.0F) * Math.cos(t / 650.0))
                        + (float) (MathUtils.randomValue(12.0F, 18.0F) * Math.sin(t / 80.0))
                          * (float) (MathUtils.randomValue(2.0F, 3.0F) * Math.cos(t / 2650.0));

                float noisePitch = ThreadLocalRandom.current().nextFloat(-1.0F, 1.0F)
                        + (float) (MathUtils.randomValue(2.0F, 3.0F) * Math.cos(t / 170.0))
                        + (float) (MathUtils.randomValue(3.0F, 4.0F) * Math.sin(t / 70.0))
                        + (float) (MathUtils.randomValue(1.0F, 2.0F) * Math.sin(t / 110.0))
                          * (float) (MathUtils.randomValue(1.0F, 2.0F) * Math.cos(t / 350.0));

                RotationRepo.update(
                        new Rotation(yaw + noiseYaw / 4.0F, pitch + noisePitch).adjustSensitivity(),
                        speed, speed, 40.0F, 40.0F,
                        1, 7, false
                );
            }
        }
    }
}
