package ru.arixcompany.features.module.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.player.inv.InventoryUtility;

import java.util.List;

public class AutoTotem extends Module {

    private final ValueSetting health = new ValueSetting("Здоровье").range(0, 36).setValue(16).setStep(1);
    private final BooleanSetting calcAbsorption = new BooleanSetting("Учитывать поглощение").setValue(true);
    private final BooleanSetting stopifEat = new BooleanSetting("Стоп когда ешь").setValue(true);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сейвить чаренные").setValue(true);
    private final ListSetting safety = new ListSetting("Проверки")
            .value("Падение", "Динамит", "Якорь", "Кристаллы", "Булава")
            .selected("Падение", "Динамит", "Якорь", "Кристаллы", "Булава");

    private int delay = 0;
    private int sourceSlot = -1;
    private boolean swappedByModule = false;

    public AutoTotem() {
        super("AutoTotem", Category.Combat);
        setup(health, calcAbsorption, stopifEat, saveEnchanted, safety);
    }

    @EventHandler
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.level == null) return;

        if (e.isReceive()) {
            if (e.getPacket() instanceof ClientboundAddEntityPacket spawn && spawn.getType() == EntityType.END_CRYSTAL) {
                if (safety.getSelected().contains("Кристаллы") && mc.player.distanceToSqr(spawn.getX(), spawn.getY(), spawn.getZ()) < 36) {
                    checkAndSwapInstantly();
                }
            }
            if (e.getPacket() instanceof ClientboundBlockUpdatePacket b && b.getBlockState().getBlock() == Blocks.OBSIDIAN) {
                if (safety.getSelected().contains("Кристаллы") && mc.player.distanceToSqr(b.getPos().getX(), b.getPos().getY(), b.getPos().getZ()) < 25) {
                    checkAndSwapInstantly();
                }
            }
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (stopifEat.isValue() && mc.player.isUsingItem()) return;

        if (safety.isSelected("Булава") && isMaceDanger()) {
            checkAndSwapInstantly();
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        boolean isDanger = shouldForceTotem();
        ItemStack offhand = mc.player.getOffhandItem();

        if (isDanger) {
            if (!offhand.is(Items.TOTEM_OF_UNDYING)) {
                int slot = findBestTotemSlot();
                if (slot != -1) {
                    sourceSlot = slot;
                    swappedByModule = true;
                    performSwap(slot);
                    delay = 2;
                }
            } else {
                if (saveEnchanted.isValue() && offhand.isEnchanted()) {
                    int normalSlot = findNormalTotemSlot();
                    if (normalSlot != -1) {
                        performSwap(normalSlot);
                        delay = 2;
                    }
                }
            }
        } else {
            if (swappedByModule && offhand.is(Items.TOTEM_OF_UNDYING)) {
                if (sourceSlot != -1) {
                    performSwap(sourceSlot);
                    delay = 2;
                }
                resetState();
            } else if (!offhand.is(Items.TOTEM_OF_UNDYING)) {
                resetState();
            }
        }
    }

    private void checkAndSwapInstantly() {
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        int slot = findBestTotemSlot();
        if (slot != -1) {
            sourceSlot = slot;
            swappedByModule = true;
            performSwap(slot);
        }
    }


    private boolean isMaceDanger() {
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isCreative()) continue;

             if (FriendRepo.isFriend(p)) continue;

            double diffX = p.getX() - mc.player.getX();
            double diffZ = p.getZ() - mc.player.getZ();
            double diffY = p.getY() - mc.player.getY();

            double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            if (horizontalDist < 4.0 && diffY > 1.5 && diffY < 20.0) {
                boolean hasMace = p.getMainHandItem().is(Items.MACE) || p.getOffhandItem().is(Items.MACE);

                if (hasMace) {
                    if (p.getDeltaMovement().y < -0.1 || p.fallDistance > 1.2f) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void resetState() {
        sourceSlot = -1;
        swappedByModule = false;
    }

    private int findNormalTotemSlot() {
        for (int i = 0; i <= 44; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING) && !s.isEnchanted()) return i;
        }
        return -1;
    }

    private int findBestTotemSlot() {
        int enchanted = -1;
        for (int i = 0; i <= 44; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING)) {
                if (!s.isEnchanted()) return i;
                if (enchanted == -1) enchanted = i;
            }
        }
        return enchanted;
    }

    private boolean shouldForceTotem() {
        float hp = mc.player.getHealth() + (calcAbsorption.isValue() ? mc.player.getAbsorptionAmount() : 0);
        List<String> checks = safety.getSelected();

        if (hp <= health.getValue()) return true;

        if (checks.contains("Падение") && (hp - ((mc.player.fallDistance - 3) / 2f + 3.5f) < 1.0f)) return true;

        if (checks.contains("Динамит")) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == null || !e.isAlive() || mc.player.distanceTo(e) > 7) continue;
                if (e instanceof PrimedTnt) return true;
                if (e instanceof Creeper c && c.getSwellDir() > 0) return true;
            }
        }

        if (checks.contains("Якорь")) {
            BlockPos pos = mc.player.blockPosition();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (mc.level.getBlockState(pos.offset(x, y, z)).is(Blocks.RESPAWN_ANCHOR)) return true;
                    }
                }
            }
        }

        if (checks.contains("Булава") && isMaceDanger()) return true;

        return false;
    }

    private void performSwap(int slot) {
        int containerSlot = slot < 9 ? slot + 36 : slot;

        InventoryUtility.clickSlot(containerSlot, 0, ClickType.SWAP, false);
        InventoryUtility.clickSlot(45, 0, ClickType.SWAP, false);
        InventoryUtility.clickSlot(containerSlot, 0, ClickType.SWAP, false);
    }
}