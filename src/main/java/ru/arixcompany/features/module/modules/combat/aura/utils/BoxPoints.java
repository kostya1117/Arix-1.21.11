package ru.arixcompany.features.module.modules.combat.aura.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.client.player.LocalPlayer;
import lombok.experimental.UtilityClass;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class BoxPoints implements IMinecraft {

    /**
     * @param target - Энтити (цель), чтобы мы могли получить её прошлую и текущую позицию
     * @param auraDistance - Дистанция твоей киллауры (distance.getValue())
     * @param canAttack - Готов ли удар (this.canAttack(0))
     */
    public static Vec3 getTargetVector(Entity target, double auraDistance, boolean canAttack) {
        LocalPlayer player = mc.player;

        if (player == null || target == null || mc.level == null) {
            return Vec3.ZERO;
        }

        AABB box = target.getBoundingBox();
        Vec3 eyePos = player.getEyePosition(); // в новых версиях 1.0F не нужно передавать
        double step = 0.1D;

        Vec3 bestVec = null;
        double closestDistance = Double.MAX_VALUE;

        // 1. Ищем ближайшую точку на хитбоксе (твой брутфорс)
        for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
                for (double z = box.minZ; z <= box.maxZ; z += step) {
                    Vec3 sample = new Vec3(x, y, z);
                    double dist = eyePos.distanceTo(sample); // Тут оставляем distanceTo, как в твоем коде

                    if (dist < closestDistance) {
                        closestDistance = dist;
                        bestVec = sample;
                    }
                }
            }
        }

        Vec3 check = new Vec3(
                target.getX() - target.xo,
                target.getY() - target.yo,
                target.getZ() - target.zo
        );

        Vec3 xyi = canAttack ? Vec3.ZERO : check;

        // Считаем центр бокса
        Vec3 center = new Vec3(
                (box.minX + box.maxX) / 2.0,
                (box.minY + box.maxY) / 2.0,
                (box.minZ + box.maxZ) / 2.0
        );

        // Центр с учетом предикта
        Vec3 predictedCenter = center.add(xyi);

        // 3. Логика проверок (переписал твой кусок 1 в 1)
        boolean isDistanceValid = !(eyePos.distanceTo(predictedCenter) > (auraDistance));
        boolean isNotVisible = isHitBoxNotVisible(player, eyePos, center);

        boolean condition = (bestVec == null || isDistanceValid) && (bestVec == null || isNotVisible);

        // Возвращаем итоговый вектор
        return predictedCenter;
    }

    private static boolean isHitBoxNotVisible(LocalPlayer player, Vec3 eyePos, Vec3 targetPos) {
        ClipContext context = new ClipContext(
                eyePos,
                targetPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        HitResult result = mc.level.clip(context);

        return result.getType() == HitResult.Type.BLOCK;
    }
}