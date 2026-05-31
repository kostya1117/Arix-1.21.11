package ru.arixcompany.features.module.modules.combat.aura;

import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;

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

        float maxDist = auraDist();
        double maxDistSqr = maxDist * maxDist;

        if (currentTarget != null
                && currentTarget.isAlive()
                && isValidTarget(currentTarget, maxDist)
                && mc.player.distanceTo(currentTarget) <= maxDist) {
            return;
        }

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;

            double distSqr = mc.player.distanceToSqr(living);
            if (distSqr > maxDistSqr) continue;

            if (!isValidTarget(living, maxDist)) continue;

            if (distSqr < bestScore) {
                bestScore = distSqr;
                bestTarget = living;
            }
        }

        currentTarget = bestTarget;
    }

    public float auraDist() {
        LocalPlayer p = mc.player;

        if (p.isFallFlying() && HitAura.elytraTarget.isValue()) {
            return HitAura.elytraRange.getValue();
        }

        return HitAura.attackRange.getValue() + HitAura.preRange.getValue();
    }


    private boolean isValidTarget(LivingEntity entity, float maxDist) {
        if (entity instanceof LocalPlayer) return false;
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand) return false;

        if (mc.player.distanceToSqr(entity) > (double) maxDist * maxDist) return false;

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

        if (entity instanceof Monster || entity instanceof Slime || entity instanceof Villager || entity instanceof Animal) {
            return HitAura.targets.isSelected("Мобы");
        }

        return false;
    }
}
