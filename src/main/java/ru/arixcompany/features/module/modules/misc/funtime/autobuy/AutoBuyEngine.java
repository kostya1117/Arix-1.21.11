package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AutoBuyEngine implements IMinecraft {

    private final Map<String, ItemTarget> targets;
    private final Timer clickCooldown;
    private final Timer scanWatch;
    private final Timer updateWatch;

    @Setter
    private ContainerScreen currentAuctionScreen;

    @Getter
    private boolean isBuying = false;
    private boolean waitingForConfirm = false;
    @Setter
    private int currentBalance = -1;
    @Setter
    private int clickDelay = 200;

    public AutoBuyEngine(Map<String, ItemTarget> targets,
                         Timer clickCooldown,
                         Timer scanWatch,
                         Timer updateWatch) {

        this.targets = targets;
        this.clickCooldown = clickCooldown;
        this.scanWatch = scanWatch;
        this.updateWatch = updateWatch;
    }

    public void tick(int updateDelay) {

        if (currentAuctionScreen == null) return;
        if (isBuying || waitingForConfirm) return;

        if (scanWatch.every(50))
            scanSlots();

        if (updateWatch.every(updateDelay)) {
            refreshPage();
            updateWatch.reset();
        }
    }

    private void scanSlots() {
        var handler = currentAuctionScreen.getMenu();

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getItem();

            if (slot.container == mc.player.getInventory()) continue;
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh"))
                continue;

            if (stack.isEmpty()) continue;

            analyzeAndBuy(slot, stack);

            if (isBuying) break;
        }
    }

    private void analyzeAndBuy(Slot slot, ItemStack stack) {
        int totalPrice = FuntimeUtil.getPrice(stack);
        if (totalPrice <= 0) return;

        int pricePerItem = FuntimeUtil.getPricePerItem(stack);
        if (pricePerItem <= 0) return;

        String itemName = ChatFormatting.stripFormatting(
                stack.getHoverName().getString()).toLowerCase();

        List<String> lore = getLore(stack);

        for (ItemTarget target : targets.values()) {

            if (target.getBuyPrice() <= 0) continue;

            boolean matches = false;

            if (target.getLoreKeywords() != null && !target.getLoreKeywords().isEmpty())
                matches = checkLoreFullMatch(lore, target.getLoreKeywords());

            if (!matches && target.isCheckByName()) {
                String targetName = ChatFormatting.stripFormatting(target.getDisplayName()).toLowerCase();
                if (itemName.contains(targetName)) matches = true;
            }

            if (!matches && target.isCheckByItem())
                if (stack.getItem() == target.getDisplayStack().getItem()) matches = true;

            if (matches && pricePerItem <= target.getBuyPrice()) {

                if (target.isCheckDurability() && stack.isDamageableItem()) {

                    int max = stack.getMaxDamage();
                    int damage = stack.getDamageValue();
                    int remaining = max - damage;

                    double percent = (remaining * 100.0) / max;

                    if (percent < target.getMinDurabilityPercent())
                        return;
                }

                if (currentBalance <= 0)
                    return;

                if (currentBalance < totalPrice)
                    return;

                performPurchase(slot);
                break;
            }
        }
    }

    private void performPurchase(Slot slot) {
        if (!clickCooldown.finished(clickDelay)) return;

        isBuying = true;
        waitingForConfirm = true;

        clickCooldown.reset();

        InvUtil.clickSlot(slot.index, 0, ClickType.QUICK_MOVE, false);
    }

    private void refreshPage() {

        var handler = currentAuctionScreen.getMenu();

        for (Slot slot : handler.slots) {

            String name = slot.getItem().getHoverName().getString().toLowerCase();

            if (name.contains("обновить") || name.contains("refresh")) {
                InvUtil.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    private boolean checkLoreFullMatch(List<String> itemLore, List<String> targetLore) {
        if (itemLore == null || targetLore == null) return false;

        int matches = 0;

        for (String targetLine : targetLore) {

            String cleanTarget = ChatFormatting.stripFormatting(targetLine);

            for (String itemLine : itemLore) {

                String cleanItem = ChatFormatting.stripFormatting(itemLine);

                if (cleanItem.contains(cleanTarget)) {
                    matches++;
                    break;
                }
            }
        }

        return matches >= targetLore.size();
    }

    private List<String> getLore(ItemStack stack) {

        ItemLore loreComp = stack.get(DataComponents.LORE);

        return loreComp != null
                ? loreComp.lines().stream().map(Component::getString).toList()
                : Collections.emptyList();
    }

    public void resetState() {
        isBuying = false;
        waitingForConfirm = false;
    }

    public void confirmPurchase() {
        isBuying = false;
        waitingForConfirm = false;
    }
}