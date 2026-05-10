package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudDraggable extends DraggableComponent {

    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");

    private static final int HOTBAR_TEX_WIDTH  = 182;
    private static final int HOTBAR_TEX_HEIGHT = 22;

    private static final int SLOT_WIDTH  = 20;
    private static final int SLOT_HEIGHT = 22;

    private static final int ITEM_X_OFFSET = 2;
    private static final int ITEM_Y_OFFSET = 3;

    private static final float MOD_SLOT_SIZE    = 20.0f;
    private static final float MOD_SLOT_GAP     = 2.0f;
    private static final float MOD_SLOT_RADIUS  = 4.0f;
    private static final float MOD_PAD          = 4.0f;
    private static final float MOD_PANEL_RADIUS = 6.0f;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public SelectSetting style = new SelectSetting("Стиль")
            .value("Обычный", "Модерн");

    private float actualX, actualY, actualW, actualH;

    public ArmorHudDraggable() {
        super("ArmorHUD", 0, 0, SLOT_WIDTH * 4, SLOT_HEIGHT);
        setPinned(true);
        setup(style);
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("ArmorHUD");
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= actualX && mouseX <= actualX + actualW
                && mouseY >= actualY && mouseY <= actualY + actualH;
    }

    private void syncPosition() {
        this.x = actualX;
        this.y = actualY;
        this.renderX = actualX;
        this.renderY = actualY;
        this.width = actualW;
        this.height = actualH;
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.player == null) return;

        List<SlotEntry> entries = collectEntries();
        if (entries.isEmpty()) return;

        if (style.isSelected("Модерн")) {
            renderModern(graphics, entries, alpha);
        } else {
            renderClassic(graphics, entries, alpha);
        }

        syncPosition();
    }

    private void renderClassic(GuiGraphics graphics, List<SlotEntry> entries, float alpha) {
        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        float chatLift = mc.gui != null ? mc.gui.hotbarChatOffset : 0.0F;

        int hotbarRight = guiW / 2 + 91;
        boolean customHotbar = HotbarDraggable.isCustomHotbarActive();

        int slotY;
        int startX;

        if (customHotbar) {
            float slotsW = 9 * 20.0f + 8 * 2.0f;
            float totalW = 4.0f + slotsW + 4.0f;
            float hotbarX = guiW / 2.0f - totalW / 2.0f;
            float hotbarY = guiH - 45 - chatLift;

            startX = (int)(hotbarX + totalW + 4);
            slotY  = (int)(hotbarY);
        } else {
            startX = hotbarRight + 2;
            slotY  = (int)(guiH - SLOT_HEIGHT - chatLift);
        }

        int count = entries.size();

        actualX = startX;
        actualY = slotY;
        actualW = count * SLOT_WIDTH;
        actualH = SLOT_HEIGHT;

        for (int i = 0; i < count; i++) {
            int slotX = startX + i * SLOT_WIDTH;

            renderHotbarSlot(graphics, slotX, slotY, i, count);

            SlotEntry entry = entries.get(i);
            if (!entry.stack.isEmpty()) {
                renderItem(graphics, entry.stack, slotX, slotY, i + 1);
            }
        }
    }

    private void renderModern(GuiGraphics graphics, List<SlotEntry> entries, float alpha) {
        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        float chatLift = mc.gui != null ? mc.gui.hotbarChatOffset : 0.0F;

        int count = entries.size();

        float slotsW = count * MOD_SLOT_SIZE + (count - 1) * MOD_SLOT_GAP;
        float totalW = MOD_PAD + slotsW + MOD_PAD;
        float totalH = MOD_PAD + MOD_SLOT_SIZE + MOD_PAD;

        float drawX, drawY;

        boolean customHotbar = HotbarDraggable.isCustomHotbarActive();

        if (customHotbar) {
            float hotbarSlotsW = 9 * 20.0f + 8 * 2.0f;
            float hotbarTotalW = 4.0f + hotbarSlotsW + 4.0f;
            float hotbarX = guiW / 2.0f - hotbarTotalW / 2.0f;

            float hotbarTotalH = 4.0f + 5.0f + 2.0f + 5.0f + 2.0f + 20.0f + 2.0f + 5.0f + 4.0f;
            float hotbarY = guiH - hotbarTotalH - 2 - chatLift;

            drawX = hotbarX + hotbarTotalW + 4;
            drawY = hotbarY + hotbarTotalH - totalH;
        } else {
            int hotbarRight = guiW / 2 + 91;
            drawX = hotbarRight + 4;
            drawY = guiH - totalH - 2 - chatLift;
        }

        actualX = drawX;
        actualY = drawY;
        actualW = totalW;
        actualH = totalH;

        float anim = alpha;

        RenderUtils.fillRoundRect(drawX, drawY, totalW, totalH, MOD_PANEL_RADIUS,
                Colors.bgPrimary(anim * 0.88f));

        for (int i = 0; i < count; i++) {
            float slotX = drawX + MOD_PAD + i * (MOD_SLOT_SIZE + MOD_SLOT_GAP);
            float slotY = drawY + MOD_PAD;

            SlotEntry entry = entries.get(i);
            boolean hasItem = !entry.stack.isEmpty();

            int slotBg = Colors.bgElement(anim * 0.5f);
            RenderUtils.fillRoundRect(slotX, slotY, MOD_SLOT_SIZE, MOD_SLOT_SIZE,
                    MOD_SLOT_RADIUS, slotBg);

            if (hasItem) {
                int itemX = (int)(slotX + (MOD_SLOT_SIZE - 16) / 2.0f);
                int itemY = (int)(slotY + (MOD_SLOT_SIZE - 16) / 2.0f);
                graphics.renderItem(mc.player, entry.stack, itemX, itemY, i + 1);
                graphics.renderItemDecorations(mc.font, entry.stack, itemX, itemY);

                if (entry.stack.isDamageableItem()) {
                    int maxDmg = entry.stack.getMaxDamage();
                    int curDmg = entry.stack.getDamageValue();
                    float durPct = Mth.clamp(1.0f - (float) curDmg / maxDmg, 0f, 1f);

                    float barY = slotY + MOD_SLOT_SIZE - 2.5f;
                    float barW = MOD_SLOT_SIZE - 4f;
                    float barH = 1.5f;
                    float barX = slotX + 2f;

                    RenderUtils.fillRoundRect(barX, barY, barW, barH, 0.5f,
                            Colors.bgElement(anim * 0.6f));

                    int durColor = getDurabilityColor(durPct, anim);
                    if (durPct > 0.001f) {
                        RenderUtils.fillRoundRect(barX, barY, barW * durPct, barH, 0.5f, durColor);
                    }
                }
            }
        }
    }

    private List<SlotEntry> collectEntries() {
        List<SlotEntry> entries = new ArrayList<>();
        if (mc.player == null) return entries;
        Player player = mc.player;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                entries.add(new SlotEntry(stack, slot));
            }
        }
        return entries;
    }

    private void renderHotbarSlot(GuiGraphics graphics, int x, int y, int index, int total) {
        int u;
        if (total == 1) {
            u = 0;
        } else if (index == 0) {
            u = 0;
        } else if (index == total - 1) {
            u = HOTBAR_TEX_WIDTH - SLOT_WIDTH;
        } else {
            u = 20;
        }

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_SPRITE,
                HOTBAR_TEX_WIDTH,
                HOTBAR_TEX_HEIGHT,
                u, 0,
                x, y,
                SLOT_WIDTH,
                SLOT_HEIGHT
        );
    }

    private void renderItem(GuiGraphics graphics, ItemStack stack, int slotX, int slotY, int seed) {
        graphics.renderItem(mc.player, stack, slotX + ITEM_X_OFFSET, slotY + ITEM_Y_OFFSET, seed);
        graphics.renderItemDecorations(mc.font, stack, slotX + ITEM_X_OFFSET, slotY + ITEM_Y_OFFSET);
    }

    private int getDurabilityColor(float pct, float alpha) {
        int r, g, b;
        if (pct > 0.5f) {
            float t = (pct - 0.5f) * 2.0f;
            r = (int) Mth.lerp(t, 255, 80);
            g = (int) Mth.lerp(t, 200, 220);
            b = 50;
        } else {
            float t = pct * 2.0f;
            r = 255;
            g = (int) Mth.lerp(t, 50, 200);
            b = 50;
        }
        int a = (int)(Mth.clamp(alpha * 0.9f, 0f, 1f) * 255f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private record SlotEntry(ItemStack stack, EquipmentSlot slot) {}
}