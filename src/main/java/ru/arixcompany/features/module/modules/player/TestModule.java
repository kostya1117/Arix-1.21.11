package ru.arixcompany.features.module.modules.player;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component; // ИСПРАВЛЕНО: добавлен импорт
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventHandledScreen;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.features.module.setting.implement.*;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List; // ИСПРАВЛЕНО: добавлен импорт

public class TestModule extends Module {

    private final BindSetting testbind = new BindSetting("Test bind Setting");
    private final BooleanSetting testboolean = new BooleanSetting("Боалеан");
    private final ColorSetting testColor = new ColorSetting("Цвет сеттинг", Color.red.getRGB());
    private final ListSetting testList = new ListSetting("Лист").value("One", "Two", "Three", "sdsdsd", "s233", "sds", "23232").selected("One", "Two");
    private final SelectSetting testSelect = new SelectSetting("Мод").value("1Аб", "2Аб", "3Аб", "1", "sdsdsds", "sdsd", "s233232");
    private final ValueSetting valueSetting = new ValueSetting("Слайдер").range(1, 5).setStep(1);
    private final TextSetting textSetting = new TextSetting("Текст Сеттинг").setText("1");

    private String lastPrintedScreen = "";

    public TestModule() {
        super("TestModule", Category.Player);
        setup(testbind, testboolean, testColor, testList, testSelect, valueSetting, textSetting);
    }

    @EventHandler
    public void onScreen(EventHandledScreen e) {
        if (!(mc.screen instanceof ContainerScreen screen)) return;

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Название
            String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());

            // ID предмета
            String itemId = "air";
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null) itemId = key.getPath();

            // Лор
            List<String> lore = new ArrayList<>();
            ItemLore loreComp = stack.get(DataComponents.LORE);
            if (loreComp != null) {
                for (Component line : loreComp.lines()) {
                    String stripped = ChatFormatting.stripFormatting(line.getString());
                    lore.add(stripped != null ? stripped : "");
                }
            }

            // Цена
            int price = FuntimeUtil.getPrice(stack);
            int pricePerItem = FuntimeUtil.getPricePerItem(stack);

            // Вывод
            System.out.println("========================================");
            System.out.println("Название: " + name);
            System.out.println("ID: " + itemId);
            System.out.println("Кол-во: " + stack.getCount());
            System.out.println("Цена: " + price + " | За шт: " + pricePerItem);

            String itemEnum = itemId.toUpperCase();

            if (!lore.isEmpty()) {
                System.out.println("Лор:");
                for (String line : lore) {
                    System.out.println("  \"" + line + "\"");
                }

                // Готовый код для копирования
                StringBuilder loreList = new StringBuilder("List.of(");
                for (int i = 0; i < lore.size(); i++) {
                    loreList.append("\"").append(lore.get(i)).append("\""); // ИСПРАВЛЕНО: get(i) вместо getItem(i)
                    if (i < lore.size() - 1) loreList.append(", ");
                }
                loreList.append(")");

                System.out.println("Готовый код:");
                System.out.println("addTargetLore(\"" + itemId + "\", \"" + name + "\", Items."
                        + itemEnum + ", " + loreList + ", 0);");
            } else {
                System.out.println("Нет лора");
                System.out.println("Готовый код:");
                System.out.println("addTargetItem(\"" + itemId + "\", \"" + name + "\", Items."
                        + itemEnum + ", 0);");
            }

            System.out.println("========================================");
        }
    }

    @Override
    public void deactivate() {
        lastPrintedScreen = "";
        super.deactivate();
    }
}