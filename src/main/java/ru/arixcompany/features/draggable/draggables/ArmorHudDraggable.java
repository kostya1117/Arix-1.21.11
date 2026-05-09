package ru.arixcompany.features.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.draggable.DraggableComponent;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;

import java.util.ArrayList;
import java.util.List;

public class ArmorHudDraggable extends DraggableComponent {

    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");

    private static final int HOTBAR_TEX_WIDTH = 182;
    private static final int HOTBAR_TEX_HEIGHT = 22;

    private static final int SLOT_U = 0;
    private static final int SLOT_V = 0;
    private static final int SLOT_WIDTH = 22;
    private static final int SLOT_HEIGHT = 22;

    private static final int ITEM_X_OFFSET = 3;
    private static final int ITEM_Y_OFFSET = 3;
    private static final int SLOT_GAP = 0;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public SelectSetting style = new SelectSetting("Стиль")
            .value("Вертикальный", "Горизонтальный");
    public BooleanSetting showHands = new BooleanSetting("Руки");

    public ArmorHudDraggable() {
        super("ArmorHUD", 4, 40, SLOT_WIDTH, SLOT_HEIGHT * 4);
        setup(style, showHands);
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

        Player player = mc.player;
        boolean vertical = style.is("Вертикальный");

        List<SlotEntry> entries = collectEntries(
                player,
                true,
                showHands.isValue(),
                showHands.isValue()
        );

        if (entries.isEmpty()) {
            return;
        }

        int count = entries.size();

        this.width = vertical
                ? SLOT_WIDTH
                : count * SLOT_WIDTH + (count - 1) * SLOT_GAP;

        this.height = vertical
                ? count * SLOT_HEIGHT + (count - 1) * SLOT_GAP
                : SLOT_HEIGHT;

        for (int i = 0; i < entries.size(); i++) {
            SlotEntry entry = entries.get(i);

            int slotX = (int) (rx + (vertical ? 0 : i * (SLOT_WIDTH + SLOT_GAP)));
            int slotY = (int) (ry + (vertical ? i * (SLOT_HEIGHT + SLOT_GAP) : 0));

            renderVanillaHotbarSlot(graphics, slotX, slotY, i, entries.size(), vertical);

            if (!entry.stack.isEmpty()) {
                renderItemInSlot(graphics, entry.stack, slotX, slotY, i + 1);
            }
        }
    }

    private List<SlotEntry> collectEntries(Player player, boolean emptySlots, boolean mainHand, boolean offHand) {
        List<SlotEntry> entries = new ArrayList<>();

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() || emptySlots) {
                entries.add(new SlotEntry(stack, slot));
            }
        }

        if (mainHand) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() || emptySlots) {
                entries.add(new SlotEntry(stack, EquipmentSlot.MAINHAND));
            }
        }

        if (offHand) {
            ItemStack stack = player.getOffhandItem();
            if (!stack.isEmpty() || emptySlots) {
                entries.add(new SlotEntry(stack, EquipmentSlot.OFFHAND));
            }
        }

        return entries;
    }

    private void renderVanillaHotbarSlot(GuiGraphics graphics, int x, int y, int index, int total, boolean vertical) {
        int u;

        if (vertical) {
            u = 0;
        } else {
            if (index == 0) {
                u = 0;
            } else if (index == total - 1) {
                u = HOTBAR_TEX_WIDTH - SLOT_WIDTH;
            } else {
                u = 21;
            }
        }

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_SPRITE,
                HOTBAR_TEX_WIDTH,
                HOTBAR_TEX_HEIGHT,
                u,
                SLOT_V,
                x,
                y,
                SLOT_WIDTH,
                SLOT_HEIGHT
        );
    }

    private void renderItemInSlot(GuiGraphics graphics, ItemStack stack, int slotX, int slotY, int seed) {
        int itemX = slotX + ITEM_X_OFFSET;
        int itemY = slotY + ITEM_Y_OFFSET;

        graphics.renderItem(mc.player, stack, itemX, itemY, seed);
        graphics.renderItemDecorations(mc.font, stack, itemX, itemY);
    }

    private record SlotEntry(ItemStack stack, EquipmentSlot slot) {
    }
}