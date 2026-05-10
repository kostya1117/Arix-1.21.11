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
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
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

        if (currentTarget != null
                && currentTarget.isAlive()
                && isValidTarget(currentTarget)
                && mc.player.distanceTo(currentTarget) <= auraDist()) {
            return;
        }

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        float maxDist = auraDist();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > maxDist) continue;

            if (dist < bestScore) {
                bestScore = dist;
                bestTarget = living;
            }
        }

        currentTarget = bestTarget;
    }

    private float auraDist() {
        return HitAura.attackRange.getValue() + HitAura.preRange.getValue();
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof LocalPlayer) return false;
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand) return false;

        if (mc.player.distanceTo(entity) > auraDist()) return false;

        if (!HitAura.misc.isSelected("Бить через блоки") && !mc.player.hasLineOfSight(entity))
            return false;

        if (entity instanceof Player player) {
            if (!HitAura.targets.isSelected("Игроки")) return false;
            if (player.isCreative()) return false;
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

        if (entity instanceof Monster || entity instanceof Slime
                || entity instanceof Villager || entity instanceof Animal) {
            return HitAura.targets.isSelected("Мобы");
        }

        return false;
    }
}
