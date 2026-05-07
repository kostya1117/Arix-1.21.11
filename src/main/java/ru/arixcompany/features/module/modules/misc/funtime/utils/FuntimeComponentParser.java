package ru.arixcompany.features.module.modules.misc.funtime.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.apache.commons.lang3.StringUtils;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class FuntimeComponentParser extends MessageSender implements IMinecraft {
    public int getPrice(ItemStack stack) {
        final int invalidPrice = 0;
        if (stack == null || stack.getComponents().isEmpty()) {
            return invalidPrice;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return invalidPrice;
        }

        for (Component line : lore.lines()) {
            String lineText = line.getString();
            if (lineText.contains("$")) {
                String priceStr = StringUtils.substringAfter(lineText, "$").trim();
                priceStr = priceStr.replaceAll("[^0-9]", "");
                try {
                    return Integer.parseInt(priceStr);
                } catch (NumberFormatException e) {
                    return invalidPrice;
                }
            }
        }

        return invalidPrice;
    }

    public int getPricePerItem(ItemStack stack) {
        int count = stack.getCount();
        if (count <= 0) return 0;
        int price = getPrice(stack);
        return price / count;
    }

    public void addPriceToTooltip(ItemStack stack) {
        if (stack == null) return;

        int price = getPricePerItem(stack);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        String formattedPrice = decimalFormat.format(price);

        Component priceText = Component.literal(ChatFormatting.GREEN + "$ " + ChatFormatting.WHITE + "Цена за 1 шт " + ChatFormatting.GREEN + "$" + formattedPrice);

        ItemLore lore = stack.getOrDefault(DataComponents.LORE, new ItemLore(List.of()));
        List<Component> newLines = new ArrayList<>(lore.lines());

        boolean alreadyExists = newLines.stream().anyMatch(line -> line.getString().equals(priceText.getString()));

        if (!alreadyExists) {
            newLines.add(priceText);
        }

        stack.set(DataComponents.LORE, new ItemLore(newLines));
    }

    public boolean hasPrice(ItemStack stack) {
        if (stack == null || stack.getComponents().isEmpty()) {
            return false;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return false;
        }

        for (Component line : lore.lines()) {
            String lineText = line.getString();
            if (lineText.contains("$")) {
                String priceStr = StringUtils.substringAfter(lineText, "$").trim();
                priceStr = priceStr.replaceAll("[^0-9]", "");
                try {
                    Integer.parseInt(priceStr);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return false;
    }
}
