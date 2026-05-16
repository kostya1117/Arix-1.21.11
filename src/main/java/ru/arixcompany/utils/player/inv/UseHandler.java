package ru.arixcompany.utils.player.inv;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.player.PlayerUtil;

public class UseHandler implements IMinecraft {

    public enum UseMode {
        NORMAL,
        SILENT
    }

    private UseMode mode = UseMode.NORMAL;
    private int swapDelay = 100;

    public UseHandler setMode(UseMode mode) {
        this.mode = mode;
        return this;
    }

    public UseHandler setSwapDelay(int delay) {
        this.swapDelay = delay;
        return this;
    }

    public void use(Item item) {
        if (mc.player == null) return;

        int hotbarSlot = InventoryUtility.findItemInHotBar(item).slot();
        if (hotbarSlot != -1) {
            useFromHotbar(hotbarSlot, mode, swapDelay);
            return;
        }

        int invSlot = InventoryUtility.findItemInInventory(item).slot();
        if (invSlot != -1) {
            useFromInventory(invSlot, mode, swapDelay);
        }
    }

    public void useFromHotbar(int slot, UseMode mode, int swapDelay) {
        if (mc.player == null) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();

        if (mode == UseMode.SILENT) {
            useFromHotbarSilent(slot, originalSlot);
        } else {
            useFromHotbarNormal(slot, originalSlot, swapDelay);
        }
    }

    public void useFromInventory(int slot, UseMode mode, int swapDelay) {
        if (mc.player == null) return;

        int hotbarSlot = mc.player.getInventory().getSelectedSlot();

        if (mode == UseMode.SILENT) {
            useFromInventorySilent(slot, hotbarSlot);
        } else {
            useFromInventoryNormal(slot, hotbarSlot, swapDelay);
        }
    }

    private void useFromHotbarSilent(int slot, int originalSlot) {
        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        PlayerUtil.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                id,
                mc.player.getYRot(),
                mc.player.getXRot()
        ));
        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
    }

    private void useFromHotbarNormal(int slot, int originalSlot, int swapDelay) {
        new UseThread(mc.player, slot, originalSlot, swapDelay, false).start();
    }

    private void useFromInventorySilent(int slot, int hotbarSlot) {
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                slot,
                hotbarSlot,
                ClickType.SWAP,
                mc.player
        );
        PlayerUtil.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                id,
                mc.player.getYRot(),
                mc.player.getXRot()
        ));
        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                slot,
                hotbarSlot,
                ClickType.SWAP,
                mc.player
        );
    }

    private void useFromInventoryNormal(int slot, int hotbarSlot, int swapDelay) {
        new UseThread(mc.player, slot, hotbarSlot, swapDelay, true).start();
    }

    public static class UseThread extends Thread {
        private final LocalPlayer player;
        private final int slot;
        private final int originalSlot;
        private final int delay;
        private final boolean isInventory;

        public UseThread(LocalPlayer player, int slot, int originalSlot, int delay, boolean isInventory) {
            this.player = player;
            this.slot = slot;
            this.originalSlot = originalSlot;
            this.delay = delay;
            this.isInventory = isInventory;
        }

        @Override
        public void run() {
            if (isInventory) {
                MoveHandler.lockMovement("UseHandler");
            }
            try {
                if (!isInventory) {
                    InventoryUtility.switchTo(slot);
                    Thread.sleep(delay);
                    PlayerUtil.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                            InteractionHand.MAIN_HAND,
                            id,
                            player.getYRot(),
                            player.getXRot()
                    ));
                    player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    Thread.sleep(delay);
                    InventoryUtility.switchTo(originalSlot);
                } else {
                    mc.gameMode.handleInventoryMouseClick(
                            player.containerMenu.containerId,
                            slot,
                            originalSlot,
                            ClickType.SWAP,
                            player
                    );
                    Thread.sleep(delay);
                    PlayerUtil.sendSequencedPacket(id -> new ServerboundUseItemPacket(
                            InteractionHand.MAIN_HAND,
                            id,
                            player.getYRot(),
                            player.getXRot()
                    ));
                    player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    Thread.sleep(delay);
                    mc.gameMode.handleInventoryMouseClick(
                            player.containerMenu.containerId,
                            slot,
                            originalSlot,
                            ClickType.SWAP,
                            player
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (isInventory) {
                    MoveHandler.unlockMovement("UseHandler");
                }
            }
        }
    }
}
