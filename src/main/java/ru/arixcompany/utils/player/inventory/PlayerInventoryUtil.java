package ru.arixcompany.utils.player.inventory;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.repos.alerts.AlertRepo;
import ru.arixcompany.utils.IMinecraft;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    public static int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        return emptySlot != -1 ? emptySlot : findNonSwordSlot();
    }
    private static int findNonSwordSlot() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (!stack.is(ItemTags.SWORDS)
                    && !stack.is(Items.ELYTRA)
                    && mc.player.getInventory().selected != i) {

                return i;
            }
        }

        return -1;
    }

    private static int findEmptySlot() {
        for(int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getItem(i).isEmpty() && mc.player.getInventory().selected != i) {
                return i;
            }
        }

        return -1;
    }
    public static int getAxeInInventoryOrHotbar(boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        int finalSlot = -1;

        for(int i = firstSlot; i < lastSlot; ++i) {
            if (mc.player.getInventory().getItem(i).getItem() instanceof AxeItem) {
                finalSlot = i;
            }
        }

        return finalSlot;
    }

    public void closeScreen(boolean packet) {
        if (!(mc.screen instanceof InventoryScreen)) {
            mc.player.closeContainer();
        }
    }

    public void swapHand(Slot slot, InteractionHand hand, boolean updateInventory) {
        if (slot == null || slot.index == -1 || (hand.equals(InteractionHand.OFF_HAND) && !(slot.container instanceof Inventory || slot.container instanceof PlayerEnderChestContainer))) return;
        int button = hand.equals(InteractionHand.MAIN_HAND) ? mc.player.getInventory().getSelectedSlot() : 40;

        swapHand(slot, button, updateInventory);
    }

    public void swapHand(Slot slot, int button, boolean updateInventory) {
        clickSlot(slot, button, ClickType.SWAP, false);
        // if (updateInventory) PlayerInventoryUtil.updateSlots();
    }

    public void swapHand(Slot slot, int button) {
        clickSlot(slot, button, ClickType.SWAP, false);
    }
    public int getMenuSlotId(Slot slot) {
        if (slot == null || mc.player == null) return -1;
        return mc.player.containerMenu.slots.indexOf(slot);
    }
    public void clickSlot(Slot slot, int button, ClickType clickType, boolean silent) {
        int slotId = getMenuSlotId(slot);
        if (slotId != -1) {
            clickSlot(slotId, button, clickType, silent);
        }
    }

    public void clickSlot(int slotId, int buttonId, ClickType clickType, boolean silent) {
        clickSlot(mc.player.containerMenu.containerId, slotId, buttonId, clickType, silent);
    }

    public void clickSlot(int windowId, int slotId, int buttonId, ClickType clickType, boolean silent) {
        mc.gameMode.handleInventoryMouseClick(windowId, slotId, buttonId, clickType, mc.player);
    }

    public Slot getSlot(Item item) {
        return getSlot(item,s -> true);
    }

    public Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public Slot getSlot(Predicate<Slot> filter) {
        return slots().filter(filter).findFirst().orElse(null);
    }

    public Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        return slots().filter(filter).max(comparator).orElse(null);
    }

    public Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return slots().filter(s -> s.getItem().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
    }

    public Slot getSlot(List<Item> item) {
        return slots().filter(s -> item.contains(s.getItem().getItem())).findFirst().orElse(null);
    }

    public int getCount(Predicate<Slot> filter) {
        return slots().filter(filter).mapToInt(s -> s.getItem().getCount()).sum();
    }

    public Stream<Slot> slots(){
        return mc.player.containerMenu.slots.stream();
    }

    public void swapAndUse(Item item) {
        float cooldownProgress =mc.player.getCooldowns().getCooldownPercent(item.getDefaultInstance(), 0f);

        if (cooldownProgress > 0) {
            AlertRepo.warn(item.getName().getString() + " в кд ");
            return;
        }

        Slot slot = getSlot(item);
        if (slot == null) {
            AlertRepo.warn(item.getName().getString() + " не найден");
            return;
        }
        PlayerInventoryComponent.addTask(() -> swapAndUse(slot));
    }

    public void swapAndUse(Slot slot) {
        swapHand(slot, InteractionHand.MAIN_HAND, false);
        PlayerInventoryUtil.closeScreen(true);
        PlayerIntersectionUtil.useItem(InteractionHand.MAIN_HAND);
        swapHand(slot, InteractionHand.MAIN_HAND,false);
        PlayerInventoryUtil.closeScreen(true);
    }

    public void moveItem(Slot from, int to) {
        if (from != null) moveItem(from.index, to, false, false);
    }

    public void moveItem(Slot from, int to, boolean task) {
        moveItem(from, to, task, false);
    }

    public void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
        if (from != null) moveItem(from.index, to, task, updateInventory);
    }

    public void moveItem(int from, int to, boolean task, boolean updateInventory) {
        if (from == to || from == -1) return;

        int count = Math.toIntExact(slots().count()) - 9;
        if (from >= count && count == 36) {
            if (task) PlayerInventoryComponent.addTask(() -> clickSlot(to, from - count, ClickType.SWAP, false));
            else {
                clickSlot(to, from - count, ClickType.SWAP, false);
                PlayerInventoryUtil.closeScreen(true);
            }
            return;
        }

        if (task) PlayerInventoryComponent.addTask(() -> moveItem(from, to, updateInventory));
        else {
            moveItem(from, to, updateInventory);
            PlayerInventoryUtil.closeScreen(true);
        }
    }

    public void moveItem(int from, int to, boolean updateInventory) {
        clickSlot(from, 0, ClickType.SWAP, false);
        clickSlot(to, 0, ClickType.SWAP, false);
        clickSlot(from, 0, ClickType.SWAP, false);
        if (updateInventory) updateSlots();
    }

    public Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getItemBySlot(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return PlayerInventoryUtil.getSlot(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE));
        else return PlayerInventoryUtil.getSlot(Items.ELYTRA);
    }
}