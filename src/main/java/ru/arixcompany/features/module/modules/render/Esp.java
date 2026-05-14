package ru.arixcompany.features.module.modules.render;

import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.Render3dUtils;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Esp extends Module {

    private final ListSetting mode = new ListSetting("Отображать")
            .value("Игроки", "Предметы");

    private final BooleanSetting boxes = new BooleanSetting("Боксы")
            .visible(() -> mode.isSelected("Игроки"));

    private final SelectSetting boxDimension = new SelectSetting("Измерение")
            .value("3D", "2D")
            .visible(() -> mode.isSelected("Игроки") && boxes.isValue());

    private final SelectSetting boxStyle3D = new SelectSetting("Стиль 3D")
            .value("Контур", "Заливка", "Оба")
            .visible(() -> mode.isSelected("Игроки") && boxes.isValue() && boxDimension.isSelected("3D"));

    private final SelectSetting boxStyle2D = new SelectSetting("Стиль 2D")
            .value("Полный", "Углы", "Хп бар")
            .visible(() -> mode.isSelected("Игроки") && boxes.isValue() && boxDimension.isSelected("2D"));

    private final BooleanSetting stackItems = new BooleanSetting("Стакать предметы")
            .visible(() -> mode.isSelected("Предметы"));

    private final BooleanSetting shulkerContents = new BooleanSetting("Содержимое шалкера")
            .setValue(true)
            .visible(() -> mode.isSelected("Предметы"));

    private final Map<Player, double[]> entityPositions = new HashMap<>();
    private final List<ItemGroup> itemGroups = new ArrayList<>();

    private static final double STACK_RADIUS = 1.5;

    public Esp() {
        super("Esp", Category.Render);
        setup(mode, boxes, boxDimension, boxStyle3D, boxStyle2D, stackItems, shulkerContents);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        float tickDelta = e.getTickDelta();

        entityPositions.clear();
        itemGroups.clear();

        if (mode.isSelected("Игроки")) {
            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;

                double x = Mth.lerp(tickDelta, player.xo, player.getX());
                double y = Mth.lerp(tickDelta, player.yo, player.getY());
                double z = Mth.lerp(tickDelta, player.zo, player.getZ());

                Vec3 head = ProjectUtils.worldSpaceToScreenSpace(new Vec3(x, y + player.getBbHeight() + 0.3, z));
                Vec3 feet = ProjectUtils.worldSpaceToScreenSpace(new Vec3(x, y, z));

                if (head == null || head.z < 0 || head.z > 1) continue;
                if (feet == null || feet.z < 0 || feet.z > 1) continue;

                entityPositions.put(player, new double[]{head.x, head.y, feet.x, feet.y});

                if (boxes.isValue() && boxDimension.isSelected("3D")) {
                    AABB box = getInterpolatedAABB(player, tickDelta);
                    Color mainColor = Arix.getInstance().getCurrentTheme().getMain();

                    switch (boxStyle3D.getSelected()) {
                        case "Контур" -> Render3dUtils.renderOutline(
                                e.getMatrixStack(), box,
                                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 255),
                                false
                        );
                        case "Заливка" -> Render3dUtils.renderFilled(
                                e.getMatrixStack(), box,
                                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 50),
                                false
                        );
                        case "Оба" -> Render3dUtils.renderFilledWithOutline(
                                e.getMatrixStack(), box,
                                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 50),
                                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 255),
                                false
                        );
                    }
                }
            }
        }

        if (mode.isSelected("Предметы")) {
            List<ItemEntity> items = mc.level.getEntitiesOfClass(
                    ItemEntity.class,
                    mc.player.getBoundingBox().inflate(64)
            );

            for (ItemEntity item : items) {
                float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                double ix = Mth.lerp(pt, item.xo, item.getX());
                double iy = Mth.lerp(pt, item.yo, item.getY());
                double iz = Mth.lerp(pt, item.zo, item.getZ());

                if (stackItems.isValue()) {
                    boolean grouped = false;
                    for (ItemGroup group : itemGroups) {
                        double dx = ix - group.anchorX;
                        double dy = iy - group.anchorY;
                        double dz = iz - group.anchorZ;
                        if (dx * dx + dy * dy + dz * dz < STACK_RADIUS * STACK_RADIUS) {
                            group.addItem(item.getItem().copy(), item.getItem().getCount());
                            grouped = true;
                            break;
                        }
                    }
                    if (!grouped) {
                        ItemGroup group = new ItemGroup(ix, iy, iz);
                        group.addItem(item.getItem().copy(), item.getItem().getCount());
                        itemGroups.add(group);
                    }
                } else {
                    ItemGroup group = new ItemGroup(ix, iy, iz);
                    group.addItem(item.getItem().copy(), item.getItem().getCount());
                    itemGroups.add(group);
                }
            }
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.level == null) return;

        if (mode.isSelected("Предметы")) {
            renderItemGroups(e.getGuiGraphics());
        }

        if (mode.isSelected("Игроки")) {
            for (Map.Entry<Player, double[]> entry : entityPositions.entrySet()) {
                Player player = entry.getKey();
                double[] pos = entry.getValue();

                float headX = (float) pos[0];
                float headY = (float) pos[1];
                float feetX = (float) pos[2];
                float feetY = (float) pos[3];

                if (boxes.isValue() && boxDimension.isSelected("2D")) {
                    render2DBox(e.getGuiGraphics(), player, headX, headY, feetX, feetY);
                }

                renderNametag(e, player, headX, headY);
            }
        }
    }

    private void renderItemGroups(GuiGraphics g) {
        float fontSize = 10;

        for (ItemGroup group : itemGroups) {
            Vec3 screen = ProjectUtils.worldSpaceToScreenSpace(
                    new Vec3(group.anchorX, group.anchorY + 0.3, group.anchorZ)
            );
            if (screen == null || screen.z < 0 || screen.z > 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            List<ItemGroup.Entry> entries = group.getEntries();
            Color mainColor = Arix.getInstance().getCurrentTheme().getMain();

            if (entries.size() == 1) {
                ItemGroup.Entry entry = entries.get(0);
                Component name = entry.stack.getHoverName();
                String countStr = entry.totalCount > 1 ? " x" + entry.totalCount : "";

                float nameWidth = FontManager.get(fontSize).getComponentWidth(name);
                float countWidth = countStr.isEmpty() ? 0 : FontManager.get(fontSize).getWidth(countStr);
                float textWidth = nameWidth + countWidth;

                float iconSize = 14;
                float iconPadding = 4;
                float padding = 4;

                List<ItemStack> shulkerItems = getShulkerContents(entry.stack);
                boolean hasShulker = shulkerContents.isValue() && !shulkerItems.isEmpty();

                float shulkerRowH = hasShulker ? (9 * 2f + 4f) : 0f;
                float rectW = padding + iconSize + iconPadding + textWidth + padding;
                float rectH = 16 + (hasShulker ? shulkerRowH + 2f : 0f);

                RenderUtils.fillRoundRect(sx - rectW / 2f, sy - rectH / 2f, rectW, rectH, 4f, 0x90000000);

                g.pose().pushMatrix();
                g.pose().translate(sx - rectW / 2f + padding, sy - rectH / 2f + (rectH - iconSize) / 2f - (hasShulker ? shulkerRowH / 2f : 0f));
                g.pose().scale(iconSize / 16f, iconSize / 16f);
                g.renderItem(entry.stack, 0, 0);
                g.renderItemDecorations(mc.font, entry.stack, 0, 0);
                g.pose().popMatrix();

                float textX = sx - rectW / 2f + padding + iconSize + iconPadding;
                float textY = sy - rectH / 2f + (rectH - FontManager.get(fontSize).getHeight() - (hasShulker ? shulkerRowH + 2f : 0f)) / 2f;

                FontManager.get(fontSize).drawComponent(g, name, textX, textY, 0xFFFFFFFF);

                if (entry.totalCount > 1) {
                    FontManager.get(fontSize).drawString(g, countStr, textX + nameWidth, textY, mainColor.getRGB());
                }

                // Рендер содержимого шалкера
                if (hasShulker) {
                    renderShulkerContents(g, shulkerItems, sx - rectW / 2f + padding,
                            sy - rectH / 2f + rectH - shulkerRowH - 2f, rectW - padding * 2f);
                }
            } else {
                float padding = 5f;
                float lineHeight = 16f;
                float gap = 2f;

                float maxTextWidth = 0;
                for (ItemGroup.Entry entry : entries) {
                    String name = entry.stack.getHoverName().getString();
                    String countStr = entry.totalCount > 1 ? " x" + entry.totalCount : "";
                    float w = FontManager.get(fontSize).getWidth(name + countStr);
                    if (w > maxTextWidth) maxTextWidth = w;
                }

                float rectW = padding + 16 + 4 + maxTextWidth + padding;

                float shulkerExtraH = 0f;
                List<List<ItemStack>> shulkerContentsList = new ArrayList<>();
                for (ItemGroup.Entry entry : entries) {
                    List<ItemStack> sc = shulkerContents.isValue() ? getShulkerContents(entry.stack) : List.of();
                    shulkerContentsList.add(sc);
                    if (!sc.isEmpty()) shulkerExtraH += 9 * 2f + 4f + 2f;
                }

                float rectH = padding + entries.size() * lineHeight + (entries.size() - 1) * gap + shulkerExtraH + padding;

                float rectX = sx - rectW / 2f;
                float rectY = sy - rectH / 2f;

                RenderUtils.fillRoundRect(rectX, rectY, rectW, rectH, 5f, 0x90000000);

                float currentY = rectY + padding;
                for (int ei = 0; ei < entries.size(); ei++) {
                    ItemGroup.Entry entry = entries.get(ei);

                    g.pose().pushMatrix();
                    g.pose().translate(rectX + padding, currentY);
                    g.pose().scale(0.85f, 0.85f);
                    g.renderItem(entry.stack, 0, 0);
                    g.renderItemDecorations(mc.font, entry.stack, 0, 0);
                    g.pose().popMatrix();

                    Component name = entry.stack.getHoverName();
                    String countStr = entry.totalCount > 1 ? " x" + entry.totalCount : "";

                    float textX = rectX + padding + 16 + 4;
                    float textY = currentY + 4;

                    FontManager.get(fontSize).drawComponent(g, name, textX, textY, 0xFFFFFFFF);

                    if (entry.totalCount > 1) {
                        float nameW = FontManager.get(fontSize).getComponentWidth(name);
                        FontManager.get(fontSize).drawString(g, countStr, textX + nameW, textY, mainColor.getRGB());
                    }

                    currentY += lineHeight + gap;

                    List<ItemStack> sc = shulkerContentsList.get(ei);
                    if (!sc.isEmpty()) {
                        float shulkerRowH = 9 * 2f + 4f;
                        renderShulkerContents(g, sc, rectX + padding, currentY, rectW - padding * 2f);
                        currentY += shulkerRowH + 2f;
                    }
                }
            }
        }
    }

    private List<ItemStack> getShulkerContents(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        if (!(stack.getItem() instanceof BlockItem bi)) return List.of();
        if (!(bi.getBlock() instanceof ShulkerBoxBlock)) return List.of();

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return List.of();

        List<ItemStack> result = new ArrayList<>();
        contents.nonEmptyItems().forEach(result::add);
        return result;
    }

    private void renderShulkerContents(GuiGraphics g, List<ItemStack> items, float x, float y, float maxWidth) {
        float iconSize = 9f;
        float spacing  = 1f;
        int   maxPerRow = Math.min(9, (int)(maxWidth / (iconSize + spacing)));
        int   count     = Math.min(items.size(), maxPerRow);

        float totalW = count * (iconSize + spacing) - spacing;
        float startX = x + (maxWidth - totalW) / 2f;

        for (int i = 0; i < count; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            float ix = startX + i * (iconSize + spacing);
            g.pose().pushMatrix();
            g.pose().translate(ix, y);
            g.pose().scale(iconSize / 16f, iconSize / 16f);
            g.renderItem(s, 0, 0);
            g.pose().popMatrix();
        }
    }

    private void render2DBox(GuiGraphics g, Player player, float headX, float headY, float feetX, float feetY) {
        Color mainColor = Arix.getInstance().getCurrentTheme().getMain();
        int color = mainColor.getRGB();

        float height = Math.abs(feetY - headY);
        float width = height * 0.5f;
        float x = headX - width / 2f;
        float y = headY;
        float lineW = 1.5f;

        switch (boxStyle2D.getSelected()) {
            case "Полный" -> drawFullBox(x, y, width, height, color, lineW);
            case "Углы" -> drawCornerBox(x, y, width, height, color, lineW);
            case "Хп бар" -> {
                drawFullBox(x, y, width, height, color, lineW);
                drawHealthBar(player, x, y, height);
            }
        }
    }

    private void drawFullBox(float x, float y, float w, float h, int color, float lw) {
        RenderUtils.fillRect(x, y, w, lw, color);
        RenderUtils.fillRect(x, y + h - lw, w, lw, color);
        RenderUtils.fillRect(x, y, lw, h, color);
        RenderUtils.fillRect(x + w - lw, y, lw, h, color);
    }

    private void drawCornerBox(float x, float y, float w, float h, int color, float lw) {
        float cl = Math.min(w, h) * 0.25f;

        RenderUtils.fillRect(x, y, cl, lw, color);
        RenderUtils.fillRect(x, y, lw, cl, color);

        RenderUtils.fillRect(x + w - cl, y, cl, lw, color);
        RenderUtils.fillRect(x + w - lw, y, lw, cl, color);

        RenderUtils.fillRect(x, y + h - lw, cl, lw, color);
        RenderUtils.fillRect(x, y + h - cl, lw, cl, color);

        RenderUtils.fillRect(x + w - cl, y + h - lw, cl, lw, color);
        RenderUtils.fillRect(x + w - lw, y + h - cl, lw, cl, color);
    }

    private void drawHealthBar(Player player, float x, float y, float h) {
        float hp = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);

        int hpColor;
        if (hp > 0.6f) hpColor = new Color(68, 255, 68).getRGB();
        else if (hp > 0.3f) hpColor = new Color(255, 204, 0).getRGB();
        else hpColor = new Color(255, 68, 68).getRGB();

        float barX = x - 4f;
        float barW = 2f;

        RenderUtils.fillRect(barX, y, barW, h, new Color(0, 0, 0, 150).getRGB());

        float filledH = h * hp;
        RenderUtils.fillRect(barX, y + (h - filledH), barW, filledH, hpColor);
    }

    private void renderNametag(EventRender2D e, Player player, float screenX, float screenY) {
        Component displayName = player.getDisplayName();
        float health = player.getHealth();

        Component nameComponent = displayName.copy()
                .append(Component.literal(" [").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.1f", health)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY));

        float fontSize = 10;
        float textWidth = FontManager.get(fontSize).getComponentWidth(nameComponent);
        float textHeight = FontManager.get(fontSize).getHeight();
        float padding = 4f;

        float rectX = screenX - textWidth / 2f - padding;
        float rectY = screenY - textHeight - 8f;

        boolean isFriend = FriendRepo.isFriend(player);
        int bgColor = isFriend ? Colors.friend(95) : 0x90000000;

        RenderUtils.fillRoundRect(rectX, rectY, textWidth + padding * 2f, textHeight + 4f, 4f, bgColor);

        FontManager.get(fontSize).drawComponent(
                e.getGuiGraphics(), nameComponent,
                screenX - textWidth / 2f, rectY + 2f, 0xFFFFFFFF
        );

        renderEquipment(e.getGuiGraphics(), player, screenX, rectY - 12f);
    }

    private void renderEquipment(GuiGraphics g, Player player, float centerX, float y) {
        float size = 14f;
        float spacing = 2f;
        List<ItemStack> stacks = new ArrayList<>();

        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.HEAD));
        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.CHEST));
        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.LEGS));
        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.FEET));
        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.MAINHAND));
        addIfNotEmpty(stacks, player.getItemBySlot(EquipmentSlot.OFFHAND));

        if (stacks.isEmpty()) return;

        float totalWidth = stacks.size() * (size + spacing) - spacing;
        float startX = centerX - totalWidth / 2f;

        for (ItemStack stack : stacks) {
            renderItem(g, stack, startX, y, size);
            startX += size + spacing;
        }
    }

    private void addIfNotEmpty(List<ItemStack> list, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) list.add(stack);
    }

    private void renderItem(GuiGraphics g, ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty()) return;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 16f, size / 16f);
        g.renderItem(stack, 0, 0);
        g.renderItemDecorations(mc.font, stack, 0, 0);
        g.pose().popMatrix();
    }

    private AABB getInterpolatedAABB(LivingEntity entity, float tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());

        float halfWidth = entity.getBbWidth() / 2.0f;
        float height = entity.getBbHeight();

        return new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + height, z + halfWidth
        );
    }

    private static class ItemGroup {
        double anchorX, anchorY, anchorZ;
        private final Map<String, Entry> entriesMap = new HashMap<>();
        private final List<Entry> entriesList = new ArrayList<>();

        ItemGroup(double x, double y, double z) {
            this.anchorX = x;
            this.anchorY = y;
            this.anchorZ = z;
        }

        void addItem(ItemStack stack, int count) {
            String key = stack.getHoverName().getString();
            Entry existing = entriesMap.get(key);
            if (existing != null) {
                existing.totalCount += count;
            } else {
                Entry entry = new Entry(stack, count);
                entriesMap.put(key, entry);
                entriesList.add(entry);
            }
        }

        List<Entry> getEntries() {
            return entriesList;
        }

        @AllArgsConstructor
        static class Entry {
            ItemStack stack;
            int totalCount;
        }
    }
}