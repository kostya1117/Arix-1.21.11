package ru.arixcompany.features.module.modules.render;

import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.utils.render.Render3dUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StorageEsp extends Module {

    private final ListSetting style = new ListSetting("Стиль")
            .value("Контур", "Заливка", "Оба")
            .selected("Оба");

    private final BooleanSetting chest = new BooleanSetting("Сундук").setValue(true);
    private final BooleanSetting trappedChest = new BooleanSetting("Сундук-ловушка").setValue(true);
    private final BooleanSetting echest = new BooleanSetting("Эндер-сундук").setValue(true);
    private final BooleanSetting shulker = new BooleanSetting("Шалкер").setValue(true);
    private final BooleanSetting barrel = new BooleanSetting("Бочка").setValue(false);
    private final BooleanSetting furnace = new BooleanSetting("Печь").setValue(false);
    private final BooleanSetting dispenser = new BooleanSetting("Раздатчик").setValue(false);
    private final BooleanSetting hopper = new BooleanSetting("Воронка").setValue(false);
    private final BooleanSetting cart = new BooleanSetting("Вагонетка").setValue(false);
    private final BooleanSetting frame = new BooleanSetting("Рамка").setValue(false);

    private static final Color C_CHEST = new Color(255, 200, 50, 255);
    private static final Color C_TRAPPED = new Color(220, 60, 60, 255);
    private static final Color C_ECHEST = new Color(130, 60, 220, 255);
    private static final Color C_SHULKER = new Color(180, 80, 255, 255);
    private static final Color C_BARREL = new Color(160, 100, 40, 255);
    private static final Color C_FURNACE = new Color(255, 130, 30, 255);
    private static final Color C_DISPENSER = new Color(160, 160, 160, 255);
    private static final Color C_HOPPER = new Color(100, 140, 180, 255);
    private static final Color C_CART = new Color(60, 180, 220, 255);
    private static final Color C_FRAME = new Color(230, 230, 230, 255);
    private static final Color C_FRAME_SHULKER = new Color(180, 80, 255, 255);

    public StorageEsp() {
        super("StorageEsp",Category.Render);
        setup(style,
                new GroupSetting("Показывать",
                        chest, trappedChest, echest, shulker, barrel, furnace, dispenser, hopper, cart, frame));
    }

    private static Color fill(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 40);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        boolean doFill = style.isSelected("Заливка") || style.isSelected("Оба");
        boolean doOutline = style.isSelected("Контур") || style.isSelected("Оба");

        for (BlockEntity be : getBlockEntities()) {
            AABB box = getBox(be);
            Color col = getColor(be);
            if (box == null || col == null) continue;

            if (doFill) Render3dUtils.renderFilled(e.getMatrixStack(), box, fill(col), false);
            if (doOutline) Render3dUtils.renderOutline(e.getMatrixStack(), box, col, false);
        }

        for (var entity : mc.level.entitiesForRendering()) {

            if (cart.isValue() && entity instanceof MinecartChest mc2) {
                AABB box = mc2.getBoundingBox();
                if (doFill) Render3dUtils.renderFilled(e.getMatrixStack(), box, fill(C_CART), false);
                if (doOutline) Render3dUtils.renderOutline(e.getMatrixStack(), box, C_CART, false);
            }

            if (frame.isValue() && (entity instanceof ItemFrame || entity instanceof GlowItemFrame)) {
                ItemFrame iframe = (ItemFrame) entity;
                boolean hasShulker = !iframe.getItem().isEmpty()
                        && iframe.getItem().getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof ShulkerBoxBlock;
                Color col = hasShulker ? C_FRAME_SHULKER : C_FRAME;
                AABB box = iframe.getBoundingBox();
                if (doFill) Render3dUtils.renderFilled(e.getMatrixStack(), box, fill(col), false);
                if (doOutline) Render3dUtils.renderOutline(e.getMatrixStack(), box, col, false);
            }
        }
    }

    private Color getColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return trappedChest.isValue() ? C_TRAPPED : null;
        if (be instanceof ChestBlockEntity) return chest.isValue() ? C_CHEST : null;
        if (be instanceof EnderChestBlockEntity) return echest.isValue() ? C_ECHEST : null;
        if (be instanceof ShulkerBoxBlockEntity) return shulker.isValue() ? C_SHULKER : null;
        if (be instanceof BarrelBlockEntity) return barrel.isValue() ? C_BARREL : null;
        if (be instanceof AbstractFurnaceBlockEntity) return furnace.isValue() ? C_FURNACE : null;
        if (be instanceof DispenserBlockEntity) return dispenser.isValue() ? C_DISPENSER : null;
        if (be instanceof HopperBlockEntity) return hopper.isValue() ? C_HOPPER : null;
        return null;
    }

    private AABB getBox(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chestBox(be);
        if (be instanceof ChestBlockEntity) return chestBox(be);
        if (be instanceof EnderChestBlockEntity) return chestBox(be);
        if (be instanceof ShulkerBoxBlockEntity) return new AABB(be.getBlockPos());
        if (be instanceof BarrelBlockEntity) return new AABB(be.getBlockPos());
        if (be instanceof AbstractFurnaceBlockEntity) return new AABB(be.getBlockPos());
        if (be instanceof DispenserBlockEntity) return new AABB(be.getBlockPos());
        if (be instanceof HopperBlockEntity) return new AABB(be.getBlockPos());
        return null;
    }

    private static AABB chestBox(BlockEntity be) {
        double x = be.getBlockPos().getX();
        double y = be.getBlockPos().getY();
        double z = be.getBlockPos().getZ();
        return new AABB(x + 0.06, y, z + 0.06, x + 0.94, y + 0.875, z + 0.94);
    }

    private List<BlockEntity> getBlockEntities() {
        List<BlockEntity> list = new ArrayList<>();
        if (mc.level == null || mc.player == null) return list;

        int viewDist = mc.options.renderDistance().get();
        int cx = (int) mc.player.getX() >> 4;
        int cz = (int) mc.player.getZ() >> 4;

        for (int dx = -viewDist; dx <= viewDist; dx++) {
            for (int dz = -viewDist; dz <= viewDist; dz++) {
                LevelChunk chunk = mc.level.getChunkSource()
                        .getChunk(cx + dx, cz + dz, ChunkStatus.FULL, false);
                if (chunk != null) {
                    list.addAll(chunk.getBlockEntities().values());
                }
            }
        }
        return list;
    }
}

