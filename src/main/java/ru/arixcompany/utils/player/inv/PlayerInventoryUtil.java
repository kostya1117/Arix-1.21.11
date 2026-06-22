package ru.arixcompany.utils.player.inv;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.experimental.UtilityClass;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import ru.arixcompany.utils.IMinecraft;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

@UtilityClass
public class PlayerInventoryUtil implements IMinecraft {

    public void updateSlots() {
        AbstractContainerMenu screenHandler = mc.player.containerMenu;
        mc.player.connection.send(new ServerboundContainerClickPacket(
                screenHandler.containerId,
                screenHandler.getStateId(),
                (short) 0,
                (byte) 0,
                ClickType.PICKUP_ALL,
                Int2ObjectMaps.emptyMap(),
                HashedStack.EMPTY
        ));
    }

    public void closeScreen(boolean packet) {
        if (packet) mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        else mc.player.closeContainer();
    }

    public void swapHand(Slot slot, InteractionHand hand, boolean updateInventory) {
        if (slot == null || slot.index == -1 || (hand.equals(InteractionHand.OFF_HAND) && !(slot.container instanceof Inventory || slot.container instanceof PlayerEnderChestContainer))) return;
        int button = hand.equals(InteractionHand.MAIN_HAND) ? mc.player.getInventory().getSelectedSlot() : 40;

        swapHand(slot, button, updateInventory);
    }

    public void swapHand(Slot slot, int button, boolean updateInventory) {
        clickSlot(slot, button, ClickType.SWAP, false);
        if (updateInventory) PlayerInventoryUtil.updateSlots();
    }

    public void clickSlot(Slot slot, int button, ClickType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.index, button, clickType, silent);
    }

    public void clickSlot(int slotId, int buttonId, ClickType clickType, boolean silent) {
        clickSlot(mc.player.containerMenu.containerId, slotId, buttonId, clickType, silent);
    }

    public void clickSlot(int windowId, int slotId, int buttonId, ClickType clickType, boolean silent) {
        mc.gameMode.handleInventoryMouseClick(windowId, slotId, buttonId, clickType, mc.player);
    }

    public Slot getSlot(Item item) {
        return getSlot(item, s -> true);
    }

    public Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return slots().filter(s -> s.getItem().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
    }

    public Stream<Slot> slots() {
        return mc.player.containerMenu.slots.stream();
    }

    public void moveItem(int from, int to, boolean updateInventory) {
        clickSlot(from, 0, ClickType.SWAP, false);
        clickSlot(to, 0, ClickType.SWAP, false);
        clickSlot(from, 0, ClickType.SWAP, false);
        if (updateInventory) updateSlots();
    }
}
