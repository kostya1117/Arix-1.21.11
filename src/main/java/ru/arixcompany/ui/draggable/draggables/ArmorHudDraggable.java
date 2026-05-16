package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;

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

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private float actualX, actualY, actualW, actualH;

    public ArmorHudDraggable() {
        super("ArmorHUD", 0, 0, SLOT_WIDTH * 4, SLOT_HEIGHT);
        setPinned(true);
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Броня");
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

        renderClassic(graphics, entries, alpha);

        syncPosition();
    }

    private void renderClassic(GuiGraphics graphics, List<SlotEntry> entries, float alpha) {
        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        float chatLift = mc.gui != null ? mc.gui.hotbarChatOffset : 0.0F;

        int hotbarRight = guiW / 2 + 91;

        int slotY;
        int startX;

        startX = hotbarRight + 2;
        slotY  = (int)(guiH - SLOT_HEIGHT - chatLift);

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

    private List<SlotEntry> collectEntries() {
        List<SlotEntry> entries = new ArrayList<>();
        if (mc.player == null) return entries;
        Player player = mc.player;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
                entries.add(new SlotEntry(stack, slot));
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

    private record SlotEntry(ItemStack stack, EquipmentSlot slot) {}
}