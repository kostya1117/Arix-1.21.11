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

import java.util.ArrayList;
import java.util.List;

/**
 * ArmorHUD — горизонтальный, статично справа от хотбара.
 * Поднимается вместе с хотбаром при открытии чата.
 * Не перетаскивается (pinned).
 *
 * Позиция считается в renderDraggable через guiWidth/guiHeight из GuiGraphics
 * — те же значения что использует ванильный хотбар, поэтому рассинхрона нет.
 */
public class ArmorHudDraggable extends DraggableComponent {

    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");

    private static final int HOTBAR_TEX_WIDTH  = 182;
    private static final int HOTBAR_TEX_HEIGHT = 22;

    // Ширина одного слота — как у ванильного хотбара (182 / 9 ≈ 20, но слот = 20px)
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

    // ── Видимость ─────────────────────────────────────────────────────────────

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("ArmorHUD");
    }

    // ── update: только видимость, позиция — в рендере ─────────────────────────
    // НЕ переопределяем updatePosition здесь — позиция считается в renderDraggable
    // через guiWidth/guiHeight из GuiGraphics, те же что у хотбара.

    // ── Рендер ────────────────────────────────────────────────────────────────

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.player == null) return;

        // Используем guiWidth/guiHeight из GuiGraphics — ТОЧНО те же значения что у хотбара
        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();

        // hotbarChatOffset из mc.gui — то же поле что двигает хотбар
        float chatLift = mc.gui != null ? mc.gui.hotbarChatOffset : 0.0F;

        // Правый край хотбара: center + 91 (хотбар 182px, центрирован)
        int hotbarRight = guiW / 2 + 91;

        // Y совпадает с хотбаром: guiH - 22 - chatLift
        // (хотбар рисуется на guiH-22 внутри translate(0, -chatLift))
        int slotY = (int)(guiH - SLOT_HEIGHT - chatLift);

        List<SlotEntry> entries = collectEntries();
        if (entries.isEmpty()) return;

        int count = entries.size();

        // Обновляем размер компонента для корректного isMouseOver
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

    // ── Вспомогательные ───────────────────────────────────────────────────────

    private List<SlotEntry> collectEntries() {
        List<SlotEntry> entries = new ArrayList<>();
        if (mc.player == null) return entries;
        Player player = mc.player;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            entries.add(new SlotEntry(player.getItemBySlot(slot), slot));
        }
        return entries;
    }

    /**
     * Рисуем кусок текстуры хотбара как фон слота.
     * u=0 — левый край, u=162 — правый, u=20 — средние.
     */
    private void renderHotbarSlot(GuiGraphics graphics, int x, int y, int index, int total) {
        int u;
        if (total == 1) {
            u = 0;
        } else if (index == 0) {
            u = 0;
        } else if (index == total - 1) {
            u = HOTBAR_TEX_WIDTH - SLOT_WIDTH; // 162
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

    // ── Запрещаем перетаскивание ──────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    private record SlotEntry(ItemStack stack, EquipmentSlot slot) {}
}
