package shulkerview;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ShulkerViewTooltipComponent(List<ItemStack> items, int columns, int color) implements TooltipComponent {
}
