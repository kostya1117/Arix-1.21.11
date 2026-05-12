package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import static ru.arixcompany.utils.render.ColorUtil.argb;

public class HotbarDraggable extends DraggableComponent {

    private static final float SLOT_SIZE    = 20.0f;
    private static final float SLOT_GAP     = 2.0f;
    private static final float SLOT_RADIUS  = 4.0f;
    private static final float PAD          = 4.0f;

    private static final float BAR_H        = 5.0f;
    private static final float BAR_GAP      = 2.0f;
    private static final float ARMOR_BAR_H  = 5.0f;
    private static final float BAR_RADIUS   = 2.5f;

    private static final float EXP_BAR_H    = 5.0f;
    private static final float EXP_GAP      = 2.0f;

    private static final float PANEL_RADIUS = 6.0f;

    private static final float NUM_FONT        = 6.0f;
    private static final float BAR_TEXT_FONT    = 7.0f;
    private static final float ITEM_NAME_FONT   = 10.0f;
    private static final float EXP_FONT         = 7.0f;

    private float healthAnim = 0.0f;
    private float foodAnim   = 0.0f;
    private float armorAnim  = 0.0f;
    private float expAnim    = 0.0f;

    private int       toolHighlightTimer = 0;
    private ItemStack lastToolHighlight  = ItemStack.EMPTY;

    public HotbarDraggable() {
        super("Hotbar", 0, 0, 200, 40);
        setPinned(true);
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Hotbar");
    }

    public static boolean isCustomHotbarActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Hotbar");
    }

    private void tickToolHighlight() {
        if (mc.player == null) return;
        ItemStack current = mc.player.getInventory().getSelectedItem();
        if (current.isEmpty()) {
            toolHighlightTimer = 0;
        } else if (lastToolHighlight.isEmpty()
                || !current.is(lastToolHighlight.getItem())
                || !current.getHoverName().equals(lastToolHighlight.getHoverName())) {
            toolHighlightTimer = 160;
        } else if (toolHighlightTimer > 0) {
            toolHighlightTimer--;
        }
        lastToolHighlight = current;
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        Player player = mc.player;
        if (player == null) return;

        tickToolHighlight();

        CustomFont numFont     = FontManager.get(NUM_FONT);
        CustomFont barTextFont = FontManager.get(BAR_TEXT_FONT);

        float slotsW = 9 * SLOT_SIZE + 8 * SLOT_GAP;
        float barW   = slotsW;

        float totalW = PAD + slotsW + PAD;
        float totalH = PAD
                + ARMOR_BAR_H + BAR_GAP
                + BAR_H       + BAR_GAP
                + SLOT_SIZE   + EXP_GAP
                + EXP_BAR_H   + PAD;

        this.width  = totalW;
        this.height = totalH;

        float chatOffset = net.minecraft.client.Minecraft.getInstance().gui.hotbarChatOffset;
        float drawX = graphics.guiWidth() / 2.0f - totalW / 2.0f;
        float drawY = graphics.guiHeight() - totalH - 2 - chatOffset;

        this.x = drawX; this.y = drawY;
        this.renderX = drawX; this.renderY = drawY;

        float anim = alpha;

        RenderUtils.fillRoundRect(drawX, drawY, totalW, totalH, PANEL_RADIUS,
                Colors.bgPrimary(anim * 0.88f));

        float contentX = drawX + PAD;
        float curY     = drawY + PAD;

        float armorBarY = curY;
        curY += ARMOR_BAR_H + BAR_GAP;

        float hpBarY   = curY;
        float foodBarY = curY;
        curY += BAR_H + BAR_GAP;

        float slotsY = curY;
        curY += SLOT_SIZE + EXP_GAP;

        float expBarY = curY;

        int armorValue = player.getArmorValue();
        float armorPct = Mth.clamp(armorValue / 20.0f, 0f, 1f);
        armorAnim += (armorPct - armorAnim) / 6.0f;

        RenderUtils.fillRoundRect(contentX, armorBarY, barW, ARMOR_BAR_H, BAR_RADIUS,
                Colors.bgElement(anim * 0.4f));

        if (armorAnim > 0.001f) {
            RenderUtils.fillRoundRect(contentX, armorBarY, barW * armorAnim, ARMOR_BAR_H, BAR_RADIUS,
                    argb(150, 180, 210, anim * 0.9f));
        }

        String armorText = String.valueOf(armorValue);
        float armorTextY = armorBarY + (ARMOR_BAR_H - barTextFont.getHeight()) / 2.0f;
        barTextFont.drawString(graphics, armorText,
                contentX + 2f, armorTextY,
                argb(255, 255, 255, anim * 0.85f));

        float halfW = (barW - 2f) / 2f;

        float maxHp     = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        float currentHp = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        float totalHp = currentHp + absorption;
        float hpPct     = Mth.clamp(currentHp / maxHp, 0f, 1f);
        healthAnim += (hpPct - healthAnim) / 6.0f;

        RenderUtils.fillRoundRect(contentX, hpBarY, halfW, BAR_H, BAR_RADIUS,
                Colors.bgElement(anim * 0.4f));

        if (healthAnim > 0.001f) {
            RenderUtils.fillRoundRect(contentX, hpBarY, halfW * healthAnim, BAR_H, BAR_RADIUS,
                    argb(255, 60, 60, anim * 0.9f));
        }

        if (absorption > 0.001f) {
            float absorbPct = Mth.clamp(absorption / maxHp, 0f, 1f);
            RenderUtils.fillRoundRect(contentX, hpBarY, halfW * Mth.clamp((currentHp + absorption) / maxHp, 0f, 1f), BAR_H, BAR_RADIUS,
                    argb(255, 200, 50, anim * 0.7f));
        }

        String hpText  = (int) totalHp + "/" + (int) maxHp;
        float hpTextY  = hpBarY + (BAR_H - barTextFont.getHeight()) / 2.0f;
        barTextFont.drawString(graphics, hpText,
                contentX + 2f, hpTextY,
                argb(255, 255, 255, anim * 0.85f));

        float foodBarX = contentX + halfW + 2f;
        FoodData food  = player.getFoodData();
        float foodPct  = Mth.clamp(food.getFoodLevel() / 20.0f, 0f, 1f);
        foodAnim += (foodPct - foodAnim) / 6.0f;

        RenderUtils.fillRoundRect(foodBarX, foodBarY, halfW, BAR_H, BAR_RADIUS,
                Colors.bgElement(anim * 0.4f));

        if (foodAnim > 0.001f) {
            RenderUtils.fillRoundRect(foodBarX, foodBarY, halfW * foodAnim, BAR_H, BAR_RADIUS,
                    argb(210, 150, 50, anim * 0.9f));
        }

        String foodText = food.getFoodLevel() + "/20";
        float foodTextY = foodBarY + (BAR_H - barTextFont.getHeight()) / 2.0f;
        barTextFont.drawString(graphics, foodText,
                foodBarX + 2f, foodTextY,
                argb(255, 255, 255, anim * 0.85f));

        int selectedSlot = player.getInventory().getSelectedSlot();

        for (int i = 0; i < 9; i++) {
            float slotX = contentX + i * (SLOT_SIZE + SLOT_GAP);
            float slotY = slotsY;

            boolean selected = (i == selectedSlot);
            int slotBg = selected
                    ? Colors.accent(anim * 0.35f)
                    : Colors.bgElement(anim * 0.5f);

            RenderUtils.fillRoundRect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, slotBg);

            if (selected) {
                RenderUtils.drawRoundRectOutline(slotX, slotY, SLOT_SIZE, SLOT_SIZE,
                        SLOT_RADIUS, 1.0f, Colors.accent(anim * 0.7f));
            }

            numFont.drawString(graphics, String.valueOf(i + 1),
                    slotX + 1.5f, slotY + 1.0f,
                    Colors.textInactive(anim * 0.4f));

            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                int itemX = (int)(slotX + (SLOT_SIZE - 16) / 2.0f);
                int itemY = (int)(slotY + (SLOT_SIZE - 16) / 2.0f);
                graphics.renderItem(player, stack, itemX, itemY, i + 1);
                graphics.renderItemDecorations(mc.font, stack, itemX, itemY);
            }
        }

        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty()) {
            float offhandX = contentX + 9 * (SLOT_SIZE + SLOT_GAP) + SLOT_GAP * 2;
            float offhandY = slotsY;

            RenderUtils.fillRoundRect(offhandX, offhandY, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS,
                    Colors.bgElement(anim * 0.5f));

            int itemX = (int)(offhandX + (SLOT_SIZE - 16) / 2.0f);
            int itemY = (int)(offhandY + (SLOT_SIZE - 16) / 2.0f);
            graphics.renderItem(player, offhandStack, itemX, itemY, 100);
            graphics.renderItemDecorations(mc.font, offhandStack, itemX, itemY);
        }

        float expPct = Mth.clamp(player.experienceProgress, 0f, 1f);
        expAnim += (expPct - expAnim) / 6.0f;

        RenderUtils.fillRoundRect(contentX, expBarY, barW, EXP_BAR_H, BAR_RADIUS,
                Colors.bgElement(anim * 0.4f));

        if (expAnim > 0.001f) {
            RenderUtils.fillRoundRect(contentX, expBarY, barW * expAnim, EXP_BAR_H, BAR_RADIUS,
                    argb(128, 255, 32, anim * 0.95f));
        }

        if (player.experienceLevel > 0) {
            String lvlText = String.valueOf(player.experienceLevel);
            float lvlTextY = expBarY + (EXP_BAR_H - barTextFont.getHeight()) / 2.0f;
            barTextFont.drawString(graphics, lvlText,
                    contentX + 2f, lvlTextY,
                    argb(255, 255, 255, anim * 0.85f));
        }

        renderCustomSelectedItemName(graphics, drawX, drawY, totalW, anim);
    }

    private void renderCustomSelectedItemName(GuiGraphics graphics,
                                              float hotbarX, float hotbarY,
                                              float hotbarW, float alpha) {
        if (toolHighlightTimer <= 0 || lastToolHighlight.isEmpty()) return;

        MutableComponent nameComponent = Component.empty()
                .append(lastToolHighlight.getHoverName())
                .withStyle(lastToolHighlight.getRarity().color());

        if (lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
            nameComponent.withStyle(ChatFormatting.ITALIC);
        }

        float fadeAlpha;
        if (toolHighlightTimer <= 20) {
            fadeAlpha = (toolHighlightTimer / 20.0f) * alpha;
        } else {
            fadeAlpha = alpha;
        }

        if (fadeAlpha <= 0.01f) return;

        CustomFont itemFont = FontManager.get(ITEM_NAME_FONT);
        float nameW = itemFont.getComponentWidth(nameComponent);
        float nameX = hotbarX + (hotbarW - nameW) / 2.0f;
        float nameY = hotbarY - itemFont.getHeight() - 6.0f;

        int bgColor = argb(0, 0, 0, fadeAlpha * 0.45f);
        RenderUtils.fillRoundRect(nameX - 4, nameY - 2, nameW + 8,
                itemFont.getHeight() + 4, 3f, bgColor);

        itemFont.drawComponent(graphics, nameComponent, nameX, nameY,
                argb(255, 255, 255, fadeAlpha));
    }
}