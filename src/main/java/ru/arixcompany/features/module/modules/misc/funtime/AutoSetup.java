package ru.arixcompany.features.module.modules.misc.funtime;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSetup extends Module implements IMinecraft {

    final BindSetting setupBind =
            new BindSetting("Автонастройка").setKey(GLFW.GLFW_KEY_UNKNOWN);

    final ValueSetting discountPercent =
            new ValueSetting("Скидка (%)").setValue(50).range(10, 90);

    final ValueSetting searchDuration =
            new ValueSetting("Время поиска (сек)").setValue(10).range(5, 60);

    final ValueSetting pageUpdateDelay =
            new ValueSetting("Обновление (мс)").setValue(2000).range(1000, 5000);

    boolean isSettingUp = false;
    boolean waitingForAuction = false;
    boolean retrying = false;

    int currentItemIndex = 0;
    int bestPricePerItem = Integer.MAX_VALUE;
    int refreshCount = 0;

    String currentItemId = "";

    ContainerScreen currentAuctionScreen = null;

    final Timer pageTimer = new Timer();
    final Timer refreshTimer = new Timer();
    final Timer retryTimer = new Timer();

    final List<ItemSearchData> itemSearchList = new ArrayList<>();

    static final Pattern PRICE_PATTERN =
            Pattern.compile("Цен[аaAАыЫ]?:?\\s*([\\d,\\s\\.]+)", Pattern.CASE_INSENSITIVE);

    public AutoSetup() {
        super("AutoSetup", Category.Misc);
        setup(setupBind, discountPercent, searchDuration, pageUpdateDelay);
        initItemSearchList();
    }

    private void initItemSearchList() {
        itemSearchList.add(new ItemSearchData("golden_apple", "гэпл"));
        itemSearchList.add(new ItemSearchData("enchanted_golden_apple", "чарка"));
        itemSearchList.add(new ItemSearchData("elytra", "элитры"));
        itemSearchList.add(new ItemSearchData("netherite_ingot", "незеритовый слиток"));
        itemSearchList.add(new ItemSearchData("spawner", "спавнер"));
        itemSearchList.add(new ItemSearchData("diamond", "алмаз"));
        itemSearchList.add(new ItemSearchData("beacon", "маяк"));
        itemSearchList.add(new ItemSearchData("sniffer_egg", "яйцо нюхача"));
        itemSearchList.add(new ItemSearchData("trial_key", "ключ испытаний"));
        itemSearchList.add(new ItemSearchData("dragon_head", "голова дракона"));
        itemSearchList.add(new ItemSearchData("villager_spawn_egg", "яйцо жителя"));
        itemSearchList.add(new ItemSearchData("dynamite_black", "блэк"));
        itemSearchList.add(new ItemSearchData("dynamite_white", "вайт"));
        itemSearchList.add(new ItemSearchData("silver", "серебро"));
        itemSearchList.add(new ItemSearchData("trapka", "трапка"));
        itemSearchList.add(new ItemSearchData("sphere_beast", "сфера бестии"));
        itemSearchList.add(new ItemSearchData("sphere_satyr", "сфера сатира"));
        itemSearchList.add(new ItemSearchData("sphere_chaos", "сфера хаоса"));
        itemSearchList.add(new ItemSearchData("sphere_ares", "сфера ареса"));
        itemSearchList.add(new ItemSearchData("sphere_hydra", "сфера гидры"));
        itemSearchList.add(new ItemSearchData("sphere_titan", "сфера титана"));
    }

    @Override
    public void deactivate() {
        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;
        currentAuctionScreen = null;
        super.deactivate();
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.isKeyDown(setupBind.getKey()) &&
                !isSettingUp &&
                mc.screen == null) {

            startSetup();
        }
    }

    @EventHandler
    public void onTick(EventTick e) {

        if (!isSettingUp) return;

        if (retrying && retryTimer.finished(2000)) {
            retrying = false;
            searchCurrentItem();
        }

        if (mc.screen instanceof ContainerScreen screen) {

            currentAuctionScreen = screen;

            if (waitingForAuction) {
                waitingForAuction = false;
                retrying = false;
                refreshCount = 0;
                bestPricePerItem = Integer.MAX_VALUE;
                pageTimer.reset();
                refreshTimer.reset();
                print("Меню открыто, начинаю сканирование...");
            }

            if (!waitingForAuction && !retrying &&
                    pageTimer.getTimePassed() < searchDuration.getValue() * 1000) {

                scanPrices();

                if (refreshTimer.finished((long) pageUpdateDelay.getValue())) {
                    refreshPage();
                    refreshTimer.reset();
                    refreshCount++;
                }

            } else if (!waitingForAuction && !retrying) {
                finishCurrentItem();
            }

        } else {

            if (!waitingForAuction && !retrying &&
                    currentItemIndex < itemSearchList.size()) {

                retrying = true;
                retryTimer.reset();
            }

            currentAuctionScreen = null;
        }
    }

    private void startSetup() {
        if (AutoBuy.get() == null) {
            print("AutoBuy не найден!");
            return;
        }

        isSettingUp = true;
        currentItemIndex = 0;
        retrying = false;

        print("Запущена автонастройка цен...");
        searchNextItem();
    }

    private void searchNextItem() {

        if (currentItemIndex >= itemSearchList.size()) {
            finishSetup();
            return;
        }

        searchCurrentItem();
    }

    private void searchCurrentItem() {

        ItemSearchData data = itemSearchList.get(currentItemIndex);

        currentItemId = data.itemId;
        bestPricePerItem = Integer.MAX_VALUE;

        if (mc.player != null) {
            mc.player.connection.sendCommand("ah search " + data.searchTerm);
        }

        waitingForAuction = true;
    }

    private void scanPrices() {

        if (currentAuctionScreen == null) return;

        var handler = currentAuctionScreen.getMenu();

        for (Slot slot : handler.slots) {

            if (slot.container == mc.player.getInventory()) continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh")) continue;

            List<String> lore = getLore(stack);
            int totalPrice = parsePriceFromLore(stack);

            if (totalPrice > 0) {
                int count = stack.getCount();
                int pricePerItem = totalPrice / count;

                if (pricePerItem < bestPricePerItem) {
                    bestPricePerItem = pricePerItem;
                }
            }
        }
    }

    private void refreshPage() {

        if (currentAuctionScreen == null) return;

        var handler = currentAuctionScreen.getMenu();

        for (Slot slot : handler.slots) {

            ItemStack stack = slot.getItem();
            String name = stack.getHoverName().getString().toLowerCase();

            if (name.contains("обновить") || name.contains("refresh")) {
                InvUtil.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    private void finishCurrentItem() {
        if (bestPricePerItem != Integer.MAX_VALUE && bestPricePerItem > 0) {
            int discountedPrice =
                    (int) (bestPricePerItem *
                            (1 - discountPercent.getValue() / 100.0));

            if (discountedPrice < 0) discountedPrice = 0;

            setPriceForItem(currentItemId, discountedPrice);

            print("Установлена цена " + formatPrice(discountedPrice)
                    + " для " + currentItemId);

        } else {
            print("Цена не найдена для " + currentItemId);
        }

        if (mc.screen != null) {
            mc.screen.onClose();
        }

        currentItemIndex++;

        new Thread(() -> {
            try {
                Thread.sleep(500);
                mc.execute(() -> {
                    if (currentItemIndex < itemSearchList.size()) {
                        searchNextItem();
                    } else {
                        finishSetup();
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void finishSetup() {

        isSettingUp = false;
        waitingForAuction = false;
        retrying = false;

        if (mc.screen != null) {
            mc.screen.onClose();
        }

        print("Автонастройка завершена.");
    }

    private void setPriceForItem(String itemId, int price) {
        AutoBuy autoBuy = AutoBuy.get();
        if (autoBuy != null) {
            autoBuy.setPriceForItem(itemId, price);
        }
    }

    private int parsePriceFromLore(ItemStack stack) {
//        for (String line : lore) {
//
//            String clean = ChatFormatting.stripFormatting(line);
//            if (clean == null) continue;
//
//            Matcher m = PRICE_PATTERN.matcher(clean);
//
//            if (m.find()) {
//                try {
//                    String priceStr = m.group(1)
//                            .replaceAll("[,\\s\\.]", "");
//                    return Integer.parseInt(priceStr);
//                } catch (Exception ignored) {}
//            }
//        }
//
//        return 0;

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
                    //System.err.println("Ошибка парсинга цены из строки: " + priceStr);
                    return invalidPrice;
                }
            }
        }

        return invalidPrice;
    }

    private List<String> getLore(ItemStack stack) {

        ItemLore loreComp = stack.get(DataComponents.LORE);

        if (loreComp == null) return Collections.emptyList();

        return loreComp.lines()
                .stream()
                .map(Component::getString)
                .toList();
    }

    private String formatPrice(int p) {
        if (p >= 1_000_000) return String.format("%.1fM", p/1_000_000f);
        if (p >= 1_000) return String.format("%.1fK", p/1_000f);
        return String.valueOf(p);
    }

    private static class ItemSearchData {

        final String itemId;
        final String searchTerm;

        ItemSearchData(String itemId, String searchTerm) {
            this.itemId = itemId;
            this.searchTerm = searchTerm;
        }
    }
}