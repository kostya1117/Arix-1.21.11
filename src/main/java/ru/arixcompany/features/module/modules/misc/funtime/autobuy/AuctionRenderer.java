package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuctionRenderer implements IMinecraft {
    private final List<PurchaseRecord> purchaseHistory = new ArrayList<>();

    private float historyTargetScroll = 0, historyCurrentScroll = 0;
    private float pricesTargetScroll = 0, pricesCurrentScroll = 0;

    private final int itemHeight = 40;
    private final int priceItemHeight = 12;

    private float historyX, historyY, historyW, historyH;
    private float pricesX, pricesY, pricesW, pricesH;

    public record PurchaseRecord(ItemStack stack, String customName, int buyPerOne, int count, int buyTotal, int sellTotal, long time) {
        public int profit() { return sellTotal - buyTotal; }
    }

    public void addPurchase(ItemStack stack, String customName, int buyPerOne, int count, int buyTotal, int sellTotal) {
        purchaseHistory.add(0, new PurchaseRecord(stack.copy(), customName, buyPerOne, count, buyTotal, sellTotal, System.currentTimeMillis()));
        if (purchaseHistory.size() > 50) purchaseHistory.remove(purchaseHistory.size() - 1);
    }

    public void handleScroll(double delta, double mouseX, double mouseY) {
        if (isInside(mouseX, mouseY, historyX, historyY, historyW, historyH)) {
            historyTargetScroll -= (float) (delta * 20);
        } else if (isInside(mouseX, mouseY, pricesX, pricesY, pricesW, pricesH)) {
            pricesTargetScroll -= (float) (delta * 20);
        }
    }

    public void render(GuiGraphics g, ContainerScreen screen, Map<String, ItemTarget> targets) {
        historyCurrentScroll = historyCurrentScroll + (historyTargetScroll - historyCurrentScroll) * 0.15f;
        pricesCurrentScroll = pricesCurrentScroll + (pricesTargetScroll - pricesCurrentScroll) * 0.15f;

        renderHistory(g, screen);
        renderTargetList(g, screen, targets);
    }

    public void renderHistory(GuiGraphics g, ContainerScreen screen) {
        float x = screen.leftPos - 135;
        float y = screen.topPos + 10;
        float width = 130;
        float height = 150;
        float headerH = 20;

        historyX = x; historyY = y; historyW = width; historyH = height;

        RenderUtils.fillRoundRect(x, y, width, height, 4f, 0x90000000);
        FontManager.get(10).drawString(g, "История", x + 5, y + 5, -1);

        float clipY = y + headerH;
        float clipH = height - headerH - 5;

        RenderUtils.pushClipRect(x, clipY, width, clipH);
        g.enableScissor((int) x, (int) clipY, (int) (x + width), (int) (clipY + clipH));

        float contentHeight = purchaseHistory.size() * itemHeight;
        float maxScroll = Math.max(0, contentHeight - clipH);
        if (historyTargetScroll < 0) historyTargetScroll = 0;
        if (historyTargetScroll > maxScroll) historyTargetScroll = maxScroll;

        float drawY = clipY - historyCurrentScroll;
        for (PurchaseRecord record : purchaseHistory) {
            if (drawY + itemHeight > clipY && drawY < clipY + clipH) {
                renderHistoryEntry(g, record, x + 5, drawY);
            }
            drawY += itemHeight;
        }

        g.disableScissor();
        RenderUtils.popClipRect();

        if (contentHeight > clipH) {
            renderScrollBar(g, x + width - 3, clipY, 2, clipH, historyCurrentScroll, maxScroll, contentHeight);
        }
    }

    private void renderTargetList(GuiGraphics g, ContainerScreen screen, Map<String, ItemTarget> targets) {
        float x = screen.leftPos + screen.imageWidth + 5;
        float y = screen.topPos + 60;
        float width = 120;
        float height = 130;
        float headerH = 20;

        pricesX = x; pricesY = y; pricesW = width; pricesH = height;

        RenderUtils.fillRoundRect(x, y, width, height, 4f, 0x90000000);
        FontManager.get(10).drawString(g, "Мониторинг цен", x + 5, y + 5,Color.white.getRGB());

        float clipY = y + headerH;
        float clipH = height - headerH - 5;

        RenderUtils.pushClipRect(x, clipY, width, clipH);
        g.enableScissor((int) x, (int) clipY, (int) (x + width), (int) (clipY + clipH));

        List<ItemTarget> activeTargets = targets.values().stream().filter(t -> t.getBuyPrice() > 0).toList();
        float contentHeight = activeTargets.size() * priceItemHeight;
        float maxScroll = Math.max(0, contentHeight - clipH);

        if (pricesTargetScroll < 0) pricesTargetScroll = 0;
        if (pricesTargetScroll > maxScroll) pricesTargetScroll = maxScroll;

        float drawY = clipY - pricesCurrentScroll;
        for (ItemTarget target : activeTargets) {
            String text = "§f" + target.getDisplayName() + ": §e" + FuntimeUtil.formatPrice(target.getBuyPrice());
            FontManager.get(8).drawString(g, text, x + 5, drawY, -1);
            drawY += priceItemHeight;
        }

        g.disableScissor();
        RenderUtils.popClipRect();

        if (contentHeight > clipH) {
            renderScrollBar(g, x + width - 3, clipY, 2, clipH, pricesCurrentScroll, maxScroll, contentHeight);
        }
    }

    private void renderScrollBar(GuiGraphics g, float x, float y, float w, float h, float currentScroll, float maxScroll, float contentHeight) {
        float scrollBarH = Math.max(10, (h / contentHeight) * h);
        float scrollProgress = currentScroll / maxScroll;
        float scrollBarY = y + (h - scrollBarH) * scrollProgress;
        RenderUtils.fillRoundRect(x, scrollBarY, w, scrollBarH, 1f, 0x60FFFFFF);
    }

    private void renderHistoryEntry(GuiGraphics g, PurchaseRecord record, float x, float y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.7f, 0.7f);
        g.renderItem(record.stack, 0, 0);
        g.pose().popMatrix();

        String name = record.customName + " x" + record.count;
        FontManager.get(8).drawString(g, name, x + 14, y, -1);

        String buy = "§7К: §c" + FuntimeUtil.formatPrice(record.buyTotal());
        FontManager.get(7).drawString(g, buy, x + 14, y + 8, -1);

        if (record.sellTotal() > 0) {
            String sell = "§7П: §a" + FuntimeUtil.formatPrice(record.sellTotal());
            FontManager.get(7).drawString(g, sell, x + 14, y + 16, -1);

            int profit = record.profit();
            String profitText = "§7Прибыль: " + (profit >= 0 ? "§a+" : "§c") + FuntimeUtil.formatPrice(profit);
            FontManager.get(7).drawString(g, profitText, x + 14, y + 24, -1);
        }
    }

    public void addButtons(ContainerScreen screen, boolean buy, boolean setup, Runnable tBuy, Runnable tSetup) {
        int x = screen.leftPos + screen.imageWidth + 5;
        int y = screen.topPos + 10;
        screen.addRenderableWidget(Button.builder(Component.literal("AutoBuy: " + (buy ? "§aВкл" : "§cВыкл")), b -> tBuy.run()).bounds(x, y, 100, 20).build());
        screen.addRenderableWidget(Button.builder(Component.literal("AutoSetup: " + (setup ? "§aВкл" : "§cВыкл")), b -> tSetup.run()).bounds(x, y + 25, 100, 20).build());
    }

    private boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}