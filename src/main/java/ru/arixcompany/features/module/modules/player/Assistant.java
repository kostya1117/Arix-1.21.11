package ru.arixcompany.features.module.modules.player;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.utils.player.inv.PlayerInventoryComponent;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwGetKeyName;

public class Assistant extends Module {

    //Ft
    private final BindSetting dezkaKey = new BindSetting("Дезка");
    private final BindSetting yavkaKey = new BindSetting("Явка");
    private final BindSetting ogneKey = new BindSetting("Огненый Заряд");
    private final BindSetting bozhKey = new BindSetting("Божья Аура");
    private final BindSetting trapKey = new BindSetting("Трапка");
    private final BindSetting plastKey = new BindSetting("Пласт");


    private final BindSetting windKey = new BindSetting("Заряд Ветра");
    private final BindSetting perkKey = new BindSetting("Перка");

    private record ItemDef(BindSetting bind, Item item, char renameLetter, String name) {}
    private final List<ItemDef> itemDefs = new ArrayList<>();

    public Assistant() {
        super("Assistant", Category.Combat);
        setup(new GroupSetting("Фантайм",
                        dezkaKey, yavkaKey, ogneKey, bozhKey, trapKey, plastKey)
                , windKey, perkKey);

        itemDefs.add(new ItemDef(dezkaKey, Items.ENDER_EYE, 'Я', "Дезка"));
        itemDefs.add(new ItemDef(yavkaKey, Items.SUGAR, 'Я', "Явка"));
        itemDefs.add(new ItemDef(ogneKey, Items.FIRE_CHARGE, 'С', "Огненый Заряд"));
        itemDefs.add(new ItemDef(bozhKey, Items.PHANTOM_MEMBRANE, 'У', "Божья Аура"));
        itemDefs.add(new ItemDef(trapKey, Items.NETHERITE_SCRAP, 'А', "Трапка"));
        itemDefs.add(new ItemDef(plastKey, Items.DRIED_KELP, 'П', "Пласт"));
        itemDefs.add(new ItemDef(windKey, Items.WIND_CHARGE, '\0', "Заряд Ветра"));
        itemDefs.add(new ItemDef(perkKey, Items.ENDER_PEARL, '\0', "Перка"));
    }

    public String getBindKeyForStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (ItemDef def : itemDefs) {
            if (stack.is(def.item) && (def.renameLetter == '\0' || containsRename(stack, def.renameLetter))) {
                int key = def.bind.getKey();
                if (key == -1) return null;
                return getKeyName(key);
            }
        }
        return null;
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (mc.screen != null) return;
        if (mc.player == null || mc.level == null) return;
        if (event.getAction() != 1) return;
        if (mc.player.isUsingItem()) return;

        for (ItemDef def : itemDefs) {
            if (event.isKeyDown(def.bind.getKey())) {
                useHotbarItem(def);
                break;
            }
        }
    }

    private void useHotbarItem(ItemDef def) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.is(def.item)) continue;
            if (def.renameLetter != '\0' && !containsRename(stack, def.renameLetter)) continue;

            final int slot = i;
            final int prevSlot = mc.player.getInventory().getSelectedSlot();
            PlayerInventoryComponent.addTask(() -> {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(prevSlot);
                sendOverlayMessage(Component.literal("Использовал " + ChatFormatting.GREEN + def.name));
            }, 1, 1);
            return;
        }
        sendOverlayMessage(Component.literal(ChatFormatting.RED + "Предмет не найден в хотбаре"));
    }

    private boolean containsRename(ItemStack stack, char letter) {
        if (stack.isEmpty()) return false;
        return stack.getHoverName().getString().toUpperCase().indexOf(Character.toUpperCase(letter)) >= 0;
    }

    public static String getKeyName(int key) {
        if (key == -1) return "";
        if (key < 0) {
            int btn = -key - 100;
            return "M" + (btn + 1);
        }
        String name = glfwGetKeyName(key, 0);
        if (name != null) return name;
        return switch (key) {
            case 256 -> "ESC"; case 257 -> "ENT"; case 258 -> "TAB";
            case 259 -> "BSP"; case 260 -> "INS"; case 261 -> "DEL";
            case 262 -> "LFT"; case 263 -> "RGT"; case 264 -> "UP";
            case 265 -> "DWN"; case 340 -> "LSH"; case 341 -> "LCT";
            case 342 -> "LAT"; case 343 -> "SUP"; case 344 -> "RSH";
            case 345 -> "RCT"; case 346 -> "RAT"; case 347 -> "MEN";
            default -> String.valueOf(key);
        };
    }
}
