package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.inv.InventoryUtility;

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

    private final Timer confirmWatch = new Timer();
    private boolean reopenRequested = false;

    @Getter private ItemTarget lastTarget;
    @Getter private int lastTotalPrice;

    @Getter private ItemTarget lastAttemptedTarget;
    @Getter private int lastAttemptedPrice;

    @Getter private int lastBoughtCount;
    @Getter private int lastBoughtPerItem;

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

        if (waitingForConfirm) {
            if (confirmWatch.finished(1500)) {
                failAndReopen();
            }
            return;
        }

        if (currentAuctionScreen == null) return;
        if (isBuying) return;

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
            if (!FuntimeUtil.hasPrice(stack)) continue;
            String itemNameRaw = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
            if (itemNameRaw.contains("не актуален")) continue;

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

        List<String> lore = FuntimeUtil.getLore(stack);

        for (ItemTarget target : targets.values()) {

            if (target.getBuyPrice() <= 0) continue;

            boolean matches = false;

            if (target.getLoreKeywords() != null && !target.getLoreKeywords().isEmpty())
                matches = FuntimeUtil.checkLoreFullMatch(lore, target.getLoreKeywords());

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

                this.lastAttemptedTarget = target;
                this.lastAttemptedPrice = pricePerItem;
                this.lastTarget = target;
                this.lastTotalPrice = totalPrice;
                this.lastBoughtCount = stack.getCount();
                this.lastBoughtPerItem = pricePerItem;

                performPurchase(slot);
                break;
            }
        }
    }

    private void performPurchase(Slot slot) {
        if (!clickCooldown.finished(clickDelay)) return;

        isBuying = true;
        waitingForConfirm = true;
        reopenRequested = false;

        confirmWatch.reset();
        clickCooldown.reset();

        InventoryUtility.clickSlot(slot.index, 0, ClickType.QUICK_MOVE, false);
    }

    private void refreshPage() {

        var handler = currentAuctionScreen.getMenu();

        for (Slot slot : handler.slots) {

            String name = slot.getItem().getHoverName().getString().toLowerCase();

            if (name.contains("обновить") || name.contains("refresh")) {
                InventoryUtility.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    public void failAndReopen() {
        isBuying = false;
        waitingForConfirm = false;
        reopenRequested = true;
        lastTarget = null;
        lastTotalPrice = 0;
        lastBoughtCount = 0;
    }

    public void resetState() {
        isBuying = false;
        waitingForConfirm = false;
        reopenRequested = false;
    }

    public void confirmPurchase() {
        isBuying = false;
        waitingForConfirm = false;
        reopenRequested = false;
    }
}