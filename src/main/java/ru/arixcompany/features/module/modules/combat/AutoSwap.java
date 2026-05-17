package ru.arixcompany.features.module.modules.combat;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.repos.alerts.AlertRepo;
import ru.arixcompany.utils.player.inv.InventoryUtility;

public class AutoSwap extends Module {

    private final SelectSetting swapFrom = new SelectSetting("С чего свапать")
            .value("Сфера", "Талисман");

    private final SelectSetting swapTo = new SelectSetting("На что свапать")
            .value("Сфера", "Талисман");

    private final BindSetting swapBind = new BindSetting("Кнопка");

    public AutoSwap() {
        super("AutoSwap", Category.Combat);
        setup(swapFrom, swapTo, swapBind);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (event.getAction() != 1) return;
        if (event.getKey() != swapBind.getKey()) return;

        String firstType = swapFrom.getSelected();
        String secondType = swapTo.getSelected();

        if (firstType.equals(secondType)) {
            AlertRepo.error("Выбери разные предметы для свапа");
            return;
        }

        ItemStack offhand = mc.player.getOffhandItem();

        String targetType = isMatchingItem(offhand, firstType) ? secondType : firstType;

        int targetSlot = findItemSlot(targetType);
        if (targetSlot == -1) {
            AlertRepo.error("Предмет не найден: " + targetType);
            return;
        }

        performSwap(targetSlot);
        AlertRepo.success("Свапнул на " + targetType);
    }

    private boolean isMatchingItem(ItemStack stack, String type) {
        return switch (type) {
            case "Сфера" -> isSfera(stack);
            case "Талисман" -> isTalisman(stack);
            default -> false;
        };
    }

    private boolean isTalisman(ItemStack stack) {
        return stack.is(Items.TOTEM_OF_UNDYING) && stack.isEnchanted();
    }

    public boolean isSfera(ItemStack stack) {
        return stack.is(Items.PLAYER_HEAD);
    }

    private int findItemSlot(String type) {
        for (int i = 0; i <= 44; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (isMatchingItem(stack, type)) {
                return i;
            }
        }
        return -1;
    }

    private void performSwap(int slot) {
        int containerSlot = slot < 9 ? slot + 36 : slot;

        InventoryUtility.clickSlot(containerSlot, 0, ClickType.SWAP, false);
        InventoryUtility.clickSlot(45, 0, ClickType.SWAP, false);
        InventoryUtility.clickSlot(containerSlot, 0, ClickType.SWAP, false);
    }
}