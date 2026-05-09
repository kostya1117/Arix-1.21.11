package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuctionRenderer implements IMinecraft {
    private final List<PurchaseRecord> purchaseHistory = new ArrayList<>();

    private float targetScroll = 0;
    private float currentScroll = 0;
    private final int itemHeight = 40;

    // Зоны скролла
    private float historyX, historyY, historyW, historyH;
    private float pricesX, pricesY, pricesW, pricesH;

    public record PurchaseRecord(
            ItemStack stack,
            String customName,
            int buyPerOne,
            int count,
            int buyTotal,
            int sellTotal,
            long time
    ) {
        public int profit() {
            return sellTotal - buyTotal;
        }
    }

    public void addPurchase(ItemStack stack,
                            String customName,
                            int buyPerOne,
                            int count,
                            int buyTotal,
                            int sellTotal) {

        purchaseHistory.add(0, new PurchaseRecord(
                stack.copy(),
                customName,
                buyPerOne,
                count,
                buyTotal,
                sellTotal,
                System.currentTimeMillis()
        ));

        if (purchaseHistory.size() > 50)
            purchaseHistory.remove(purchaseHistory.size() - 1);
    }

    public void handleScroll(double delta, double mouseX, double mouseY) {

        if (isInside(mouseX, mouseY, historyX, historyY, historyW, historyH)
                || isInside(mouseX, mouseY, pricesX, pricesY, pricesW, pricesH)) {

            targetScroll -= (float) (delta * 20);
        }
    }

    public void render(GuiGraphics g, ContainerScreen screen, Map<String, ItemTarget> targets) {
        currentScroll = currentScroll + (targetScroll - currentScroll) * 0.1f;

        renderHistory(g, screen);
        renderTargetList(g, screen, targets);
    }

    public void renderHistory(GuiGraphics g, ContainerScreen screen) {
        float x = screen.leftPos - 135;
        float y = screen.topPos + 10;
        float width = 130;
        float height = 150;
        historyX = x;
        historyY = y;
        historyW = width;
        historyH = height;

        RenderUtils.fillRoundRect(x, y, width, height, 4f, 0x90000000);
        FontManager.get(10).drawString(g, "История", x + 5, y + 5, -1);

        float currentY = y + 20 - currentScroll;
        for (PurchaseRecord record : purchaseHistory) {
            if (currentY + itemHeight > y + 20 && currentY < y + height) {
                renderHistoryEntry(g, record, x + 5, currentY);
            }
            currentY += itemHeight;
        }

        float contentHeight = purchaseHistory.size() * itemHeight;
        float maxScroll = Math.max(0, contentHeight - (height - 20));

        if (targetScroll > maxScroll)
            targetScroll = maxScroll;

        if (contentHeight > height - 20) {

            float scrollBarHeight = (height - 20) * ((height - 20) / contentHeight);
            float scrollProgress = currentScroll / maxScroll;

            float scrollY = y + 20 + ((height - 20 - scrollBarHeight) * scrollProgress);

            RenderUtils.fillRoundRect(
                    x + width - 4,
                    scrollY,
                    3,
                    scrollBarHeight,
                    2f,
                    0x60FFFFFF
            );
        }
    }

    private void renderHistoryEntry(GuiGraphics g, PurchaseRecord record, float x, float y) {

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.7f, 0.7f);
        g.renderItem(record.stack, 0, 0);
        g.pose().popMatrix();

        String name = record.customName + " x" + record.count;
        FontManager.get(8).drawString(g, name, x + 14, y, -1);

        String buy = "§7Куплено: §c" + FuntimeUtil.formatPrice(record.buyTotal());
        FontManager.get(7).drawString(g, buy, x + 14, y + 8, -1);

        if (record.sellTotal() > 0) {
            String sell = "§7Продано: §a" + FuntimeUtil.formatPrice(record.sellTotal());
            FontManager.get(7).drawString(g, sell, x + 14, y + 16, -1);

            int profit = record.profit();
            String profitText = "§7Профит: " +
                    (profit >= 0 ? "§a+" : "§c") +
                    FuntimeUtil.formatPrice(profit);

            FontManager.get(7).drawString(g, profitText, x + 14, y + 24, -1);
        }
    }

    private void renderTargetList(GuiGraphics g, ContainerScreen screen, Map<String, ItemTarget> targets) {
        float x = screen.leftPos + screen.imageWidth + 5;
        float y = screen.topPos + 60;
        float width = 120;
        float height = 130;
        pricesX = x;
        pricesY = y;
        pricesW = width;
        pricesH = height;

        RenderUtils.fillRoundRect(x, y, width, height, 4f, 0x90000000);
        FontManager.get(10).drawString(g, "Мониторинг цен", x + 5, y + 5, ChatFormatting.GOLD.getColor());

        float listY = y + 20;
        for (ItemTarget target : targets.values()) {
            if (target.getBuyPrice() > 0) {
                String text = "§f" + target.getDisplayName() + ": §e" + FuntimeUtil.formatPrice(target.getBuyPrice());
                FontManager.get(8).drawString(g, text, x + 5, listY, -1);
                listY += 10;
            }
        }
    }

    public void addButtons(ContainerScreen screen, boolean buy, boolean setup, Runnable tBuy, Runnable tSetup) {
        int x = screen.leftPos + screen.imageWidth + 5;
        int y = screen.topPos + 10;
        screen.addRenderableWidget(Button.builder(Component.literal("AutoBuy: " + (buy ? "§aВкл" : "§cВыкл")), b -> tBuy.run()).bounds(x, y, 100, 20).build());
        screen.addRenderableWidget(Button.builder(Component.literal("AutoSetup: " + (setup ? "§aВкл" : "§cВыкл")), b -> tSetup.run()).bounds(x, y + 25, 100, 20).build());
    }

    private boolean isInside(double mouseX, double mouseY,
                             float x, float y,
                             float width, float height) {

        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }
}