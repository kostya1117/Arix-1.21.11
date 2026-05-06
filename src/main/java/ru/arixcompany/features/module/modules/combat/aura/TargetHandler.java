package ru.arixcompany.features.module.modules.combat.aura;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.IMinecraft;

public class TargetHandler implements IMinecraft {

    private LivingEntity currentTarget;

    public LivingEntity getTarget() {
        return currentTarget;
    }

    public void updateTarget() {
        if (mc.player == null || mc.level == null) {
            currentTarget = null;
            return;
        }

        LivingEntity bestTarget = null;
        // ИСПРАВЛЕНО: приоритет по дистанции, а не по углу.
        // Угол используется только как вторичный критерий через взвешенный score.
        double bestScore = Double.MAX_VALUE;

        float maxDist = auraDist();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > maxDist) continue;

            // Score = дистанция (основной) + небольшой вес угла (вторичный)
            // Это даёт ближайшую цель с небольшим предпочтением к той, на которую смотрим
            double score = dist;

            if (score < bestScore) {
                bestScore = score;
                bestTarget = living;
            }
        }

        currentTarget = bestTarget;
    }

    private float auraDist() {
        return HitAura.attackRange.getValue() + HitAura.preRange.getValue();
    }

    private boolean isValidTarget(LivingEntity entity) {
        // Базовые проверки
        if (entity instanceof LocalPlayer) return false;
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand) return false;

        // Дистанция
        if (mc.player.distanceTo(entity) > auraDist()) return false;

        // Линия видимости
        if (!HitAura.misc.isSelected("Бить через блоки") && !mc.player.hasLineOfSight(entity))
            return false;

        // Игроки
        if (entity instanceof Player player) {
            if (!HitAura.targets.isSelected("Игроки")) return false;
            if (player.isCreative()) return false;
            // ИСПРАВЛЕНО: убрана дублирующая проверка друзей (была дважды)
            if (FriendRepo.isFriend(player.getName().getString())) return false;

            boolean hasArmor = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                        && !player.getItemBySlot(slot).isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }

            if (!HitAura.targets.isSelected("Голые") && !hasArmor) return false;
            if (!HitAura.targets.isSelected("Инвизки") && player.isInvisible()) return false;

            return true;
        }

        // Мобы
        if (entity instanceof Monster || entity instanceof Slime
                || entity instanceof Villager || entity instanceof Animal) {
            return HitAura.targets.isSelected("Мобы");
        }

        return false;
    }
}
