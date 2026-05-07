package ru.arixcompany.features.module.modules.misc.funtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.booleans.BooleanSet;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeComponentParser;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.ui.clickgui.Colors;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuy extends Module {
    public static AutoBuy instance;

    public static AutoBuy get() {
        return instance;
    }

    final BindSetting menuKey = new BindSetting("Открыть меню");
    final BindSetting autoBuyBind = new BindSetting("Включить AutoBuy" ).setKey(GLFW.GLFW_KEY_UNKNOWN);
    final ValueSetting updateDelay = new ValueSetting("Задержка обновления (мс)").setValue(500).range(100, 2000).step(100);
    final ValueSetting anarchyChangeDelay = new ValueSetting("Менять анархию каждые (мин)").setValue(5).range(3, 10).step(1);
    final ValueSetting clickDelay = new ValueSetting("Задержка между кликами (мс)").setValue(200).range(50, 500).step(50);
    final BooleanSetting checkBalance = new BooleanSetting("Проверка баланса");

    boolean autoBuyEnabled = false;
    final Map<String, ItemTarget> targets = new LinkedHashMap<>();
    boolean isScanning = false;
    boolean isBuying = false;
    boolean waitingForPurchaseConfirm = false;
    boolean hasClickedThisTick = false;

    final Timer scanWatch = new Timer();
    final Timer updateWatch = new Timer();
    final Timer anarchyWatch = new Timer();
    final Timer clickCooldown = new Timer();
    ContainerScreen currentAuctionScreen = null;

    private int currentBalance = -1;
    private boolean waitingForBalance = false;

    final List<Integer> allAnarchies = new ArrayList<>();

    static final Pattern PRICE_PATTERN = Pattern.compile("Цен[аaAАыЫ]?:?\\s*([\\d,\\s\\.]+)", Pattern.CASE_INSENSITIVE);

    public AutoBuy() {
        super("AutoBuy", Category.Misc);
        setup(menuKey, autoBuyBind, updateDelay, anarchyChangeDelay, clickDelay,checkBalance);
        initAllAnarchies();
        initDefaultTargets();
        instance = this;
    }

    public void setPriceForItem(String itemId, int price) {
        ItemTarget target = targets.get(itemId);
        if (target != null) {
            target.buyPrice = price;
            print("§aЦена для " + target.displayName + " установлена: " + formatPrice(price));
        }
    }

    private void initAllAnarchies() {
        for (int i = 103; i <= 112; i++) allAnarchies.add(i);
        for (int i = 208; i <= 231; i++) allAnarchies.add(i);
        for (int i = 305; i <= 319; i++) allAnarchies.add(i);
        for (int i = 504; i <= 512; i++) allAnarchies.add(i);
        for (int i = 901; i <= 904; i++) allAnarchies.add(i);
    }

    private void initDefaultTargets() {
        // Обычные предметы (по типу предмета)
        addTargetItem("golden_apple", "Золотое яблоко", Items.GOLDEN_APPLE, 0);
        addTargetItem("enchanted_golden_apple", "Зач. яблоко", Items.ENCHANTED_GOLDEN_APPLE, 0);
        addTargetItem("elytra", "Элитры", Items.ELYTRA, 0);
        addTargetItem("netherite_ingot", "Незерит слиток", Items.NETHERITE_INGOT, 0);
        addTargetItem("spawner", "Спавнер", Items.SPAWNER, 0);
        addTargetItem("diamond", "Алмаз", Items.DIAMOND, 0);
        addTargetItem("beacon", "Маяк", Items.BEACON, 0);
        addTargetItem("sniffer_egg", "Яйцо нюхача", Items.SNIFFER_EGG, 0);
        addTargetItem("trial_key", "Ключ испытаний", Items.TRIAL_KEY, 0);
        addTargetItem("dragon_head", "Голова дракона", Items.DRAGON_HEAD, 0);
        addTargetItem("villager_spawn_egg", "Яйцо крестьянина", Items.VILLAGER_SPAWN_EGG, 0);

        // Динамит BLACK (по лору)
        addTargetLore("dynamite_black", "Динамит BLACK", Items.TNT,
                List.of("Этот динамит взрывается", "в 10 раз сильнее обычного", "и способен взорвать обсидиан"), 0);

        // Динамит WHITE (по лору)
        addTargetLore("dynamite_white", "Динамит WHITE", Items.TNT,
                List.of("Этот динамит взрывается", "в 10 раз сильнее обычного"), 0);

        // Серебро (по лору)
        addTargetLore("silver", "Серебро", Items.IRON_NUGGET,
                List.of("Это валюта для покупки", "отмычек к тайникам", "у Знахаря (/warp stash)"), 0);

        // Трапка (по лору)
        addTargetLore("trapka", "Трапка", Items.NETHERITE_SCRAP, List.of("Нерушимая клетка"), 0);

        // Сферы (по лору)
        addTargetLore("sphere_beast", "Сфера Бестии", Items.PLAYER_HEAD,
                List.of("вериная дикая мощь", "Обостряет реакции", "Укрепляя ваше тело."), 0);
        addTargetLore("sphere_satyr", "Сфера Сатира", Items.PLAYER_HEAD,
                List.of("Шёпот Сатира звучит", "Ускоряя расправу", "Но сковывая прыжок."), 0);
        addTargetLore("sphere_chaos", "Сфера Хаоса", Items.PLAYER_HEAD, List.of("Хаос искажает реальность"), 0);
        addTargetLore("sphere_ares", "Сфера Ареса", Items.PLAYER_HEAD, List.of("Дух Ареса пылает внутри"), 0);
        addTargetLore("sphere_hydra", "Сфера Гидры", Items.PLAYER_HEAD, List.of("Живучесть темных глубин"), 0);
        addTargetLore("sphere_titan", "Сфера Титана", Items.PLAYER_HEAD, List.of("Мощь Титанов крепка"), 0);

        // Талисманы (по лору)
        addTargetLore("talisman_demon", "Талисман Демона", Items.TOTEM_OF_UNDYING,
                List.of("Печать разжигает ярость", "Ускоряя удары сердца", "И силу каждой атаки."), 0);
        addTargetLore("talisman_discord", "Талисман Раздора", Items.TOTEM_OF_UNDYING,
                List.of("Раздор жаждет хаоса", "Даруя безумный темп", "Но разрушая броню."), 0);
        addTargetLore("talisman_rage", "Талисман Ярости", Items.TOTEM_OF_UNDYING, List.of("Чистая, дикая агрессия"), 0);
        addTargetLore("talisman_crusher", "Талисман Крушителя", Items.TOTEM_OF_UNDYING, List.of("Легендарный символ"), 0);
        addTargetLore("talisman_tyrant", "Талисман Тирана", Items.TOTEM_OF_UNDYING, List.of("Тиран подавляет слабых"), 0);

        // Зелья (по названию)
        addTargetName("potion_assassin", "[★] Зелье Ассасина", Items.SPLASH_POTION, 0);
        addTargetName("potion_holy_water", "[★] Святая вода", Items.SPLASH_POTION, 0);
        addTargetName("potion_paladin", "[★] Зелье Палладина", Items.SPLASH_POTION, 0);
        addTargetName("potion_sleeping", "[★] Снотворное", Items.SPLASH_POTION, 0);
        addTargetName("potion_clapper", "[★] Хлопушка", Items.SPLASH_POTION, 0);
        addTargetName("potion_wrath", "[★] Зелье Гнева", Items.SPLASH_POTION, 0);
        addTargetName("potion_radiation", "[★] Зелье Радиации", Items.SPLASH_POTION, 0);

        // Броня Крушителя (по названию)
        addTargetName("crusher_pickaxe", "Кирка Крушителя", Items.NETHERITE_PICKAXE, 0);
        addTargetName("crusher_leggings", "Поножи Крушителя", Items.NETHERITE_LEGGINGS, 0);
        addTargetName("crusher_chestplate", "Нагрудник Крушителя", Items.NETHERITE_CHESTPLATE, 0);
        addTargetName("crusher_helmet", "Шлем Крушителя", Items.NETHERITE_HELMET, 0);
        addTargetName("crusher_boots", "Ботинки Крушителя", Items.NETHERITE_BOOTS, 0);
    }

    private void addTargetItem(String id, String displayName, Item item, int buyPrice) {
        targets.put(id, new ItemTarget(id, displayName, new ItemStack(item), null, buyPrice, false, true));
    }

    private void addTargetName(String id, String displayName, Item item, int buyPrice) {
        targets.put(id, new ItemTarget(id, displayName, new ItemStack(item), null, buyPrice, true, false));
    }

    private void addTargetLore(String id, String displayName, Item item, List<String> loreKeywords, int buyPrice) {
        targets.put(id, new ItemTarget(id, displayName, new ItemStack(item), loreKeywords, buyPrice, false, false));
    }

    @Override
    public void activate() {
        super.activate();
        anarchyWatch.reset();
        clickCooldown.reset();
        requestBalance();
    }

    @Override
    public void deactivate() {
        autoBuyEnabled = false;
        isScanning = false;
        isBuying = false;
        waitingForPurchaseConfirm = false;
        hasClickedThisTick = false;
        super.deactivate();
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.isKeyDown(menuKey.getKey()) && mc.screen == null) openMenu();

        if (e.isKeyDown(autoBuyBind.getKey()) && mc.screen == null) {
            if (mc.player != null) {
                mc.player.connection.sendCommand("ah");
                autoBuyEnabled = true;
                waitingForPurchaseConfirm = false;
                isBuying = false;
                requestBalance();
                print("§aAutoBuy включен");
            }
        }
    }

    @EventHandler
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ClientboundSystemChatPacket msg) {
            String message = msg.content().getString();

            if (message.contains("Вы успешно купили") || message.contains("куплен")) {
                if (waitingForPurchaseConfirm) {
                    waitingForPurchaseConfirm = false;
                    isBuying = false;
                    hasClickedThisTick = false;
                    print("§aПокупка подтверждена!");
                    requestBalance();

                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            mc.execute(() -> {
                                if (mc.screen != null) {
                                    mc.screen.onClose();
                                }
                            });
                            Thread.sleep(1500);
                            mc.execute(() -> {
                                if (mc.player != null && autoBuyEnabled) {
                                    mc.player.connection.sendCommand("ah");
                                }
                            });
                        } catch (Exception ignored) {}
                    }).start();
                }
            }

            if (message.contains("не хватает") || message.contains("Monet") || message.contains("монет")) {
                if (waitingForPurchaseConfirm) {
                    waitingForPurchaseConfirm = false;
                    isBuying = false;
                    hasClickedThisTick = false;
                    print("§cНе хватает денег!");

                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            mc.execute(() -> {
                                if (mc.player != null && autoBuyEnabled) {
                                    mc.player.connection.sendCommand("ah");
                                }
                            });
                        } catch (Exception ignored) {}
                    }).start();
                }
            }

            if (waitingForBalance && message.contains("Ваш баланс")) {
                Matcher m = Pattern.compile("\\$(\\d[\\d,]*)").matcher(message);

                if (m.find()) {
                    try {
                        String balanceStr = m.group(1).replaceAll("[^0-9]", "");
                        currentBalance = Integer.parseInt(balanceStr);

                        print("§eБаланс: " + formatPrice(currentBalance));
                    } catch (Exception ignored) {}
                }

                waitingForBalance = false;
            }
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!autoBuyEnabled) return;

        if (autoBuyEnabled && !isBuying && !waitingForPurchaseConfirm && anarchyWatch.finished((long) anarchyChangeDelay.getValue() * 60 * 1000)) {
            changeRandomAnarchy();
            anarchyWatch.reset();
        }

        if (waitingForPurchaseConfirm || isBuying) return;

        if (mc.screen instanceof ContainerScreen screen) {
            String title = screen.getTitle().getString().toLowerCase();
            if (title.contains("аукцион") || title.contains("auction")) {
                currentAuctionScreen = screen;
                isScanning = true;
            }
        } else {
            isScanning = false;
            currentAuctionScreen = null;
        }

        if (isScanning && currentAuctionScreen != null) {
            if (scanWatch.every(50)) scanSlots();
            if (updateWatch.every((long) updateDelay.getValue())) {
                refreshPage();
                updateWatch.reset();
            }
        }
    }

    private void changeRandomAnarchy() {
        if (mc.player == null || allAnarchies.isEmpty()) return;
        Random random = new Random();
        int randomAnarchy = allAnarchies.get(random.nextInt(allAnarchies.size()));
        mc.player.connection.sendCommand("an" + randomAnarchy);
        print("§eСмена анархии на /an" + randomAnarchy);
    }

    private void scanSlots() {
        if (currentAuctionScreen == null || isBuying || waitingForPurchaseConfirm) return;

        var handler = currentAuctionScreen.getMenu();
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.container == mc.player.getInventory()) continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || isRefreshButton(stack)) continue;

            analyzeAndBuyItem(slot, stack);
            if (isBuying) break;
        }
    }

    private void analyzeAndBuyItem(Slot slot, ItemStack stack) {
        List<String> lore = getLore(stack);

        int totalPrice = FuntimeComponentParser.getPrice(stack);
        if (totalPrice <= 0) return;

        int count = stack.getCount();
        // Используем утилку вместо ручного деления
        int pricePerItem = FuntimeComponentParser.getPricePerItem(stack);
        if (pricePerItem <= 0) return;

        String itemName = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();

        for (ItemTarget target : targets.values()) {
            if (target.buyPrice <= 0) continue;

            boolean matches = false;

            if (target.loreKeywords != null && !target.loreKeywords.isEmpty()) {
                matches = checkLoreFullMatch(lore, target.loreKeywords);
            }

            if (!matches && target.isCheckByName()) {
                String targetName = ChatFormatting.stripFormatting(target.displayName).toLowerCase();
                if (itemName.contains(targetName)) matches = true;
            }

            if (!matches && target.isCheckByItem()) {
                if (stack.getItem() == target.displayStack.getItem()) matches = true;
            }

            if (matches && pricePerItem <= target.buyPrice) {

                if (checkBalance.isValue()) {
                    if (currentBalance == -1) {
                        requestBalance();
                        return;
                    }
                    if (currentBalance < totalPrice) {
                        print("§cНедостаточно денег: " + formatPrice(totalPrice));
                        return;
                    }
                }

                performPurchase(slot, target, totalPrice, pricePerItem, count);
                break;
            }
        }
    }

    private void requestBalance() {
        if (mc.player == null) return;

        waitingForBalance = true;
        mc.player.connection.sendCommand("balance");
    }

    private boolean checkLoreFullMatch(List<String> itemLore, List<String> targetLore) {
        if (itemLore == null || targetLore == null) return false;

        int matches = 0;
        for (String targetLine : targetLore) {
            String cleanTarget = ChatFormatting.stripFormatting(targetLine);
            if (cleanTarget == null) continue;

            for (String itemLine : itemLore) {
                String cleanItem = ChatFormatting.stripFormatting(itemLine);
                if (cleanItem != null && cleanItem.contains(cleanTarget)) {
                    matches++;
                    break;
                }
            }
        }
        return matches >= targetLore.size();
    }

    private void performPurchase(Slot slot, ItemTarget target, int total, int perItem, int count) {
        if (isBuying || waitingForPurchaseConfirm) return;
        if (!clickCooldown.finished((long) clickDelay.getValue())) return;

        isBuying = true;
        clickCooldown.reset();

        InvUtil.clickSlot(slot.index, 0, ClickType.QUICK_MOVE, false);

        waitingForPurchaseConfirm = true;

        print(String.format("§eПокупка: %s x%d за %d$", target.displayName, count, total));

        new Thread(() -> {
            try {
                Thread.sleep(7000);
                if (waitingForPurchaseConfirm) {
                    mc.execute(() -> {
                        waitingForPurchaseConfirm = false;
                        isBuying = false;
                        print("§cТаймаут покупки");
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private boolean isRefreshButton(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("обновить") || name.contains("refresh");
    }

    private void refreshPage() {
        if (currentAuctionScreen == null || isBuying || waitingForPurchaseConfirm) return;
        var handler = currentAuctionScreen.getMenu();
        for (Slot slot : handler.slots) {
            if (isRefreshButton(slot.getItem())) {
                InvUtil.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    private List<String> getLore(ItemStack stack) {
        ItemLore loreComp = stack.get(DataComponents.LORE);
        return loreComp != null ? loreComp.lines().stream().map(Component::getString).toList() : Collections.emptyList();
    }

    private void openMenu() { mc.setScreen(new AutoBuyScreen()); }

    private final class AutoBuyScreen extends Screen implements IMinecraft {

        private static final float SLOT_SIZE = 42f;
        private static final float PADDING = 6f;
        private static final int COLS = 5;
        private static final int ROWS = 5;

        private float x, y;
        private final float width = COLS * (SLOT_SIZE + PADDING) + PADDING;
        private final float height = ROWS * (SLOT_SIZE + PADDING) + 40;

        private boolean dragging;
        private float dragX, dragY;

        private int scroll;
        private int maxScroll;

        protected AutoBuyScreen() {
            super(Component.literal("AutoBuy"));
        }

        @Override
        public void init() {
            x = mc.getWindow().getGuiScaledWidth() / 2f - width / 2f;
            y = mc.getWindow().getGuiScaledHeight() / 2f - height / 2f;

            int totalRows = (int) Math.ceil(targets.size() / (double) COLS);
            maxScroll = Math.max(0, totalRows - ROWS);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {

            if (dragging) {
                x = mouseX - dragX;
                y = mouseY - dragY;
            }

            float alpha = 1f;

            RenderUtils.fillRoundRect(x, y, width, height, 8f, Colors.bgSecondary(alpha));
            RenderUtils.drawRoundRectOutline(x, y, width, height, 8f, 1f, Colors.outline(alpha));

            FontManager.get(14).drawCenteredString(g,
                    "AutoBuy",
                    x + width / 2f,
                    y + 10,
                    Colors.textActive(alpha));

            int startIndex = scroll * COLS;
            int endIndex = Math.min(startIndex + ROWS * COLS, targets.size());

            List<ItemTarget> list = new ArrayList<>(targets.values());

            for (int i = startIndex; i < endIndex; i++) {

                ItemTarget item = list.get(i);
                int local = i - startIndex;
                int row = local / COLS;
                int col = local % COLS;

                float sx = x + PADDING + col * (SLOT_SIZE + PADDING);
                float sy = y + 30 + row * (SLOT_SIZE + PADDING);

                boolean active = item.buyPrice > 0;

                int bg = active ? Colors.accent(alpha) : Colors.bgSecondary(alpha);
                int outline = active ? Colors.accent(alpha) : Colors.outline(alpha);

                RenderUtils.fillRoundRect(sx, sy, SLOT_SIZE, SLOT_SIZE, 6f, bg);
                RenderUtils.drawRoundRectOutline(sx, sy, SLOT_SIZE, SLOT_SIZE, 6f, 1f, outline);

                g.renderItem(item.displayStack, (int)(sx + 12), (int)(sy + 12));

                if (active) {
                    FontManager.get(10).drawCenteredString(g,
                            formatPrice(item.buyPrice),
                            sx + SLOT_SIZE / 2f,
                            sy + SLOT_SIZE - 10,
                            Colors.textActive(alpha));
                }
            }

            super.render(g, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
            int mx = (int) event.x();
            int my = (int) event.y();
            int button = event.button();

            if (mx >= x && mx <= x + width &&
                    my >= y && my <= y + 25 &&
                    button == 0) {

                dragging = true;
                dragX = mx - x;
                dragY = my - y;
                return true;
            }

            int startIndex = scroll * COLS;
            List<ItemTarget> list = new ArrayList<>(targets.values());

            for (int i = startIndex; i < Math.min(startIndex + ROWS * COLS, list.size()); i++) {

                ItemTarget item = list.get(i);
                int local = i - startIndex;
                int row = local / COLS;
                int col = local % COLS;

                float sx = x + PADDING + col * (SLOT_SIZE + PADDING);
                float sy = y + 30 + row * (SLOT_SIZE + PADDING);

                if (mx >= sx && mx <= sx + SLOT_SIZE &&
                        my >= sy && my <= sy + SLOT_SIZE) {

                    if (button == 0) {
                        item.buyPrice = 0;
                        print("Сброшена цена для " + item.displayName);
                    }

                    if (button == 1) {
                        mc.setScreen(new EditPriceScreen(item, this));
                    }

                    return true;
                }
            }

            return super.mouseClicked(event, handled);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            dragging = false;
            return super.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            int mx = (int) event.x();
            int my = (int) event.y();

            if (dragging) {
                x = mx - this.dragX;
                y = my - this.dragY;
                return true;
            }

            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (maxScroll > 0) {
                scroll -= (int) scrollY;
                scroll = Math.max(0, Math.min(scroll, maxScroll));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private final class EditPriceScreen extends Screen implements IMinecraft {

        private final ItemTarget target;
        private final Screen parent;

        private String buffer;

        private float x, y;
        private final float width = 180;
        private final float height = 95;

        private boolean dragging;
        private float dragX, dragY;

        protected EditPriceScreen(ItemTarget target, Screen parent) {
            super(Component.literal("Edit Price"));
            this.target = target;
            this.parent = parent;
            this.buffer = target.buyPrice > 0 ? String.valueOf(target.buyPrice) : "";
        }

        @Override
        public void init() {
            x = mc.getWindow().getGuiScaledWidth() / 2f - width / 2f;
            y = mc.getWindow().getGuiScaledHeight() / 2f - height / 2f;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {

            if (dragging) {
                x = mouseX - dragX;
                y = mouseY - dragY;
            }

            float alpha = 1f;

            RenderUtils.fillRoundRect(x, y, width, height, 8f, Colors.bgSecondary(alpha));
            RenderUtils.drawRoundRectOutline(x, y, width, height, 8f, 1f, Colors.outline(alpha));

            FontManager.get(13).drawCenteredString(g,
                    target.displayName,
                    x + width / 2f,
                    y + 12,
                    Colors.textActive(alpha));

            float boxX = x + 10;
            float boxY = y + 38;

            RenderUtils.fillRoundRect(boxX, boxY, width - 20, 24, 6f, Colors.bgSecondary(alpha));
            RenderUtils.drawRoundRectOutline(boxX, boxY, width - 20, 24, 6f, 1f, Colors.accent(alpha));

            FontManager.get(12).drawString(g,
                    buffer.isEmpty() ? "0" : buffer,
                    boxX + 6,
                    boxY + 6,
                    Colors.textActive(alpha));

            super.render(g, mouseX, mouseY, delta);
        }

        // ============= INPUT =============

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
            int mx = (int) event.x();
            int my = (int) event.y();
            int button = event.button();

            if (mx >= x && mx <= x + width &&
                    my >= y && my <= y + 25 &&
                    button == 0) {

                dragging = true;
                dragX = mx - x;
                dragY = my - y;
                return true;
            }

            return super.mouseClicked(event, handled);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            dragging = false;
            return super.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            int mx = (int) event.x();
            int my = (int) event.y();

            if (dragging) {
                x = mx - this.dragX;
                y = my - this.dragY;
                return true;
            }

            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int key = event.key();

            if (key == 259 && !buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
                return true;
            }

            if (key == 257) {
                apply();
                return true;
            }

            if (key == 256) {
                mc.setScreen(parent);
                return true;
            }

            return super.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            char c = (char) event.codepoint();

            if (Character.isDigit(c) && buffer.length() < 9) {
                buffer += c;
                return true;
            }

            return super.charTyped(event);
        }

        private void apply() {
            try {
                int price = buffer.isEmpty() ? 0 : Integer.parseInt(buffer);
                target.buyPrice = price;

                print(target.displayName + " цена установлена: " + formatPrice(price));

            } catch (Exception ignored) {}

            mc.setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private String formatPrice(int p) {
        if (p >= 1_000_000) return String.format("%.1fM", p/1_000_000f);
        if (p >= 1_000) return String.format("%.1fK", p/1_000f);
        return String.valueOf(p);
    }

    @Getter
    public static class ItemTarget {
        final String id, displayName;
        final ItemStack displayStack;
        final List<String> loreKeywords;
        final boolean checkByName;
        final boolean checkByItem;
        int buyPrice;

        public ItemTarget(String id, String displayName, ItemStack stack, List<String> lore, int price, boolean checkByName, boolean checkByItem) {
            this.id = id;
            this.displayName = displayName;
            this.displayStack = stack;
            this.loreKeywords = lore;
            this.buyPrice = price;
            this.checkByName = checkByName;
            this.checkByItem = checkByItem;
        }
    }
}