package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

@UtilityClass
public class AuraUtil implements IMinecraft {

    public double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public boolean validDistance(Entity entity, float distance) {
        return getStrictDistance(entity) < distance;
    }

    public Vec3 getClosestVec(Entity entity) {
        Vec3 eyePos = mc.player.getEyePosition();
        return getClosestVec(eyePos, entity).subtract(eyePos);
    }

    public Vec3 getClosestVec(Vec3 vec, AABB aabb) {
        return new Vec3(
                Mth.clamp(vec.x, aabb.minX, aabb.maxX),
                Mth.clamp(vec.y, aabb.minY, aabb.maxY),
                Mth.clamp(vec.z, aabb.minZ, aabb.maxZ)
        );
    }

    public static void breakShield(LivingEntity target) {
        if (target.isBlocking() && target.getActiveItem().getItem() == Items.SHIELD) {
            int invSlot = PlayerInventoryUtil.getAxeInInventoryOrHotbar(false);
            int hotBarSlot = PlayerInventoryUtil.getAxeInInventoryOrHotbar(true);

            if (hotBarSlot == -1 && invSlot != -1) {
                int bestSlot = PlayerInventoryUtil.findBestSlotInHotBar();
                mc.gameMode.handleInventoryMouseClick(0, invSlot, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(0, bestSlot + 36, 0, ClickType.PICKUP, mc.player);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(bestSlot));
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
                mc.gameMode.handleInventoryMouseClick(0, bestSlot + 36, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(0, invSlot, 0, ClickType.PICKUP, mc.player);
            }

            if (hotBarSlot != -1) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotBarSlot));
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
            }
        }
    }

    public Vec3 getClosestVec(Vec3 vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) rotationYaw += 180.0F;

        float forward = 1.0F;
        if (moveForward < 0.0F) forward = -0.5F;
        else if (moveForward > 0.0F) forward = 0.5F;

        if (moveStrafing > 0.0F) rotationYaw -= 90.0F * forward;
        if (moveStrafing < 0.0F) rotationYaw += 90.0F * forward;

        return Math.toRadians(rotationYaw);
    }
}
