package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.features.module.modules.render.Interface;

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
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("ArmorHUD");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.player == null) return;

        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        float chatLift = mc.gui != null ? mc.gui.hotbarChatOffset : 0.0F;

        int hotbarRight = guiW / 2 + 91;

        int slotY = (int)(guiH - SLOT_HEIGHT - chatLift);

        List<SlotEntry> entries = collectEntries();
        if (entries.isEmpty()) return;

        int count = entries.size();

        this.width  = count * SLOT_WIDTH;
        this.height = SLOT_HEIGHT;

        for (int i = 0; i < count; i++) {
            int slotX = hotbarRight + 2 + i * SLOT_WIDTH;

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
            entries.add(new SlotEntry(player.getItemBySlot(slot), slot));
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


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    private record SlotEntry(ItemStack stack, EquipmentSlot slot) {}
}
