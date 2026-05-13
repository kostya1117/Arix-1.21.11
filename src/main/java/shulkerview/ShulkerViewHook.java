package shulkerview;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.Optional;

public final class ShulkerViewHook {

    private ShulkerViewHook() {}

    public static Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();

        if (stack.has(DataComponents.CONTAINER_LOOT)) return Optional.empty();

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return Optional.empty();

        ContainerInfo info = getContainerInfo(stack);
        if (info == null) return Optional.empty();

        NonNullList<ItemStack> items = NonNullList.withSize(info.size, ItemStack.EMPTY);
        contents.copyInto(items);

        boolean hasItems = items.stream().anyMatch(s -> !s.isEmpty());
        if (!hasItems) return Optional.empty();

        int color = getColor(stack);

        return Optional.of(new ShulkerViewTooltipComponent(items, info.columns, color));
    }

    private static ContainerInfo getContainerInfo(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return null;

        var block = bi.getBlock();

        if (block instanceof ShulkerBoxBlock) return new ContainerInfo(27, 9);

        return switch (block) {
            case net.minecraft.world.level.block.TrappedChestBlock ignored   -> new ContainerInfo(27, 9);
            case net.minecraft.world.level.block.ChestBlock ignored          -> new ContainerInfo(27, 9);
            case net.minecraft.world.level.block.BarrelBlock ignored         -> new ContainerInfo(27, 9);
            case net.minecraft.world.level.block.FurnaceBlock ignored        -> new ContainerInfo(3,  3);
            case net.minecraft.world.level.block.BlastFurnaceBlock ignored   -> new ContainerInfo(3,  3);
            case net.minecraft.world.level.block.SmokerBlock ignored         -> new ContainerInfo(3,  3);
            case net.minecraft.world.level.block.DropperBlock ignored        -> new ContainerInfo(9,  3);
            case net.minecraft.world.level.block.DispenserBlock ignored      -> new ContainerInfo(9,  3);
            case net.minecraft.world.level.block.HopperBlock ignored         -> new ContainerInfo(5,  5);
            case net.minecraft.world.level.block.BrewingStandBlock ignored   -> new ContainerInfo(5,  5);
            case net.minecraft.world.level.block.ChiseledBookShelfBlock ignored -> new ContainerInfo(6, 6);
            default -> null;
        };
    }

    private static int getColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return 0xFF_FFFFFF;
        if (!(bi.getBlock() instanceof ShulkerBoxBlock shulker)) return 0xFF_FFFFFF;

        DyeColor dye = shulker.getColor();
        if (dye == null) return 0xFF_976797;

        int raw = dye.getTextureDiffuseColor();
        int r = Math.min(255, (int)(((raw >> 16) & 0xFF) * 1.2f));
        int g = Math.min(255, (int)(((raw >>  8) & 0xFF) * 1.2f));
        int b = Math.min(255, (int)(( raw        & 0xFF) * 1.2f));
        return 0xFF_000000 | (r << 16) | (g << 8) | b;
    }

    private record ContainerInfo(int size, int columns) {}
}
