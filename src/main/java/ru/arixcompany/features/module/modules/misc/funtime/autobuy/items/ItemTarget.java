package ru.arixcompany.features.module.modules.misc.funtime.autobuy.items;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Setter
@Getter
public  class ItemTarget {

    private final String id;
    private final String displayName;
    private final ItemStack displayStack;
    private final List<String> loreKeywords;
    private final boolean checkByName;
    private final boolean checkByItem;
    private final String searchTerm;
    private boolean checkDurability = false;
    private int minDurabilityPercent = 0;
    private int buyPrice;

    public ItemTarget(String id,
                      String displayName,
                      ItemStack stack,
                      List<String> lore,
                      int price,
                      boolean checkByName,
                      boolean checkByItem,
                      String searchTerm) {

        this.id = id;
        this.displayName = displayName;
        this.displayStack = stack;
        this.loreKeywords = lore;
        this.buyPrice = price;
        this.checkByName = checkByName;
        this.checkByItem = checkByItem;
        this.searchTerm = searchTerm;
    }
}
