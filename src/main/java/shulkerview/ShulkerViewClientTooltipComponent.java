package shulkerview;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.List;

public class ShulkerViewClientTooltipComponent implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;
    private static final int PADDING   = 7;

    private final List<ItemStack> items;
    private final int columns;
    private final int color;

    public ShulkerViewClientTooltipComponent(ShulkerViewTooltipComponent data) {
        this.items   = data.items();
        this.columns = Math.max(1, data.columns());
        this.color   = data.color();
    }

    private int rows() {
        return (int) Math.ceil(items.size() / (double) columns);
    }

    @Override
    public int getWidth(Font font) {
        return PADDING * 2 + columns * SLOT_SIZE;
    }

    @Override
    public int getHeight(Font font) {
        return PADDING * 2 + rows() * SLOT_SIZE + 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, int viewportWidth, int viewportHeight, GuiGraphics graphics) {
        int w = getWidth(font);
        int h = getHeight(font);

        int bgColor = blendWithAlpha(color, 0xC0);
        RenderUtils.fillRoundRect(graphics, x, y, w, h, 4f, bgColor);

        int slotBg = 0x30_000000;
        for (int i = 0; i < items.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int sx  = x + PADDING + col * SLOT_SIZE;
            int sy  = y + PADDING + row * SLOT_SIZE;
            RenderUtils.fillRoundRect(graphics, sx, sy, SLOT_SIZE - 1, SLOT_SIZE - 1, 2f, slotBg);
        }

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            int col = i % columns;
            int row = i / columns;
            int sx  = x + PADDING + col * SLOT_SIZE + 1;
            int sy  = y + PADDING + row * SLOT_SIZE + 1;

            graphics.renderItem(stack, sx, sy);
            graphics.renderItemDecorations(font, stack, sx, sy);
        }
    }

    private static int blendWithAlpha(int argb, int alpha) {
        return (argb & 0x00_FFFFFF) | (alpha << 24);
    }
}
