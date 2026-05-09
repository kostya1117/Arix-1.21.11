package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.AutoBuy;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;

public class AutoSellEngine implements IMinecraft {

    @Getter
    private boolean selling = false;

    public void processSell(ItemTarget target, int boughtTotalPrice, int marginPercent) {

        if (selling) return;
        selling = true;

        new Thread(() -> {
            try {

                log("§fНачинаю продажу: §e" + target.getDisplayName());

                int boughtPerItem = boughtTotalPrice;

                final int[] slotFound = {-1};
                final int[] stackSize = {0};

                mc.execute(() -> {
                    if (mc.screen != null) mc.setScreen(null);

                    slotFound[0] = findBoughtItem(target);

                    if (slotFound[0] != -1) {
                        ItemStack stack = mc.player.getInventory().getItem(slotFound[0]);
                        stackSize[0] = stack.getCount();
                    }
                });

                sleep(600);

                if (slotFound[0] == -1) {
                    log("Предмет не найден в инвентаре.");
                    selling = false;
                    return;
                }

                mc.execute(() -> {
                    if (mc.screen != null) mc.setScreen(null);
                    mc.player.connection.sendCommand("ah search " + target.getSearchTerm());
                });

                long waitLimit = System.currentTimeMillis() + 5000;

                while (!(mc.screen instanceof ContainerScreen)
                        && System.currentTimeMillis() < waitLimit) {
                    sleep(100);
                }

                if (!(mc.screen instanceof ContainerScreen screen)) {
                    log("GUI аукциона не открылось.");
                    selling = false;
                    return;
                }

                int minPricePerItem = Integer.MAX_VALUE;

                for (Slot slot : screen.getMenu().slots) {

                    if (slot.container == mc.player.getInventory()) continue;

                    ItemStack stack = slot.getItem();
                    if (stack.isEmpty()) continue;
                    if (!FuntimeUtil.hasPrice(stack)) continue;

                    String name = ChatFormatting.stripFormatting(
                            stack.getHoverName().getString()).toLowerCase();

                    if (name.contains("не актуален")) continue;

                    if (!checkMatch(stack, target)) continue;

                    int price = FuntimeUtil.getPricePerItem(stack);

                    if (price > 0 && price < minPricePerItem) {
                        minPricePerItem = price;
                    }
                }

                int sellPerItem;

                if (minPricePerItem == Integer.MAX_VALUE) {

                    sellPerItem = (int) (boughtPerItem * (1 + marginPercent / 100.0));

                } else {

                    int undercut = minPricePerItem - 500;
                    int minProfit = (int) (boughtPerItem * 1.03);

                    sellPerItem = Math.max(undercut, minProfit);
                }

                int finalPrice = sellPerItem * stackSize[0];

                mc.execute(() -> {
                    if (mc.screen != null) mc.setScreen(null);
                });

                sleep(500);

                mc.execute(() -> {
                    mc.player.connection.sendCommand("ah sell " + finalPrice);
                    MessageSender.print("§aВыставлено за §e"
                            + FuntimeUtil.formatPrice(finalPrice));
                });

                AutoBuy.auctionRenderer.addPurchase(
                        target.getDisplayStack(),
                        target.getDisplayName(),
                        boughtPerItem,
                        stackSize[0],
                        boughtTotalPrice,
                        finalPrice
                );

                sleep(800);

                mc.execute(() -> mc.player.connection.sendCommand("ah"));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                selling = false;
            }

        }).start();
    }

    private boolean checkMatch(ItemStack stack, ItemTarget target) {

        String name = ChatFormatting.stripFormatting(
                stack.getHoverName().getString()).toLowerCase();

        if (target.getLoreKeywords() != null && !target.getLoreKeywords().isEmpty()) {
            if (FuntimeUtil.checkLoreFullMatch(
                    FuntimeUtil.getLore(stack),
                    target.getLoreKeywords())) return true;
        }

        if (target.isCheckByItem()
                && stack.getItem() == target.getDisplayStack().getItem())
            return true;

        return name.contains(target.getSearchTerm().toLowerCase());
    }

    private int findBoughtItem(ItemTarget target) {

        for (int i = 0; i < 36; i++) {

            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (checkMatch(stack, target)) return i;
        }

        return -1;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {}
    }

    private void log(String msg) {
        MessageSender.print(msg);
    }
}