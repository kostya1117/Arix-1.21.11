package ru.arixcompany.features.module.modules.render;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.render.EventScreen;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nametags extends Module {

    private final ListSetting mode =  new ListSetting("Кого рендерить")
                    .value("Игроки", "Предметы");

    private final Map<Player, double[]> entityPositions = new HashMap<>();

    public Nametags() {
        super("Nametags", Category.Render);
        setup(mode);
    }

    @EventHandler
    public void onRender3D(EventRender3D e) {

        if (mc.level == null || mc.player == null) return;

        entityPositions.clear();

        for (Player player : mc.level.players()) {

            if (player == mc.player) continue;
            if (player.isInvisible()) continue;

            double x = Mth.lerp(e.getTickDelta(), player.xo, player.getX());
            double y = Mth.lerp(e.getTickDelta(), player.yo, player.getY());
            double z = Mth.lerp(e.getTickDelta(), player.zo, player.getZ());

            Vec3 head = ProjectUtils.worldSpaceToScreenSpace(
                    new Vec3(x, y + player.getBbHeight() + 0.3, z)
            );

            if (head == null || head.z < 0 || head.z > 1) continue;

            entityPositions.put(player, new double[]{head.x, head.y});
        }
    }

    @EventHandler
    public void onRender2D(EventScreen e) {
        if (mc.level == null) return;

        if (mode.isSelected("Предметы")) {
            renderWorldItems(e.getGuiGraphics());
        }

        if (mode.isSelected("Игроки")) {

            for (Map.Entry<Player, double[]> entry : entityPositions.entrySet()) {

                Player player = entry.getKey();
                double[] pos = entry.getValue();

                float screenX = (float) pos[0];
                float screenY = (float) pos[1];

                Component displayName = player.getDisplayName();
                float health = player.getHealth();

                Component nameComponent = displayName.copy()
                        .append(net.minecraft.network.chat.Component.literal(" [")
                                .withStyle(ChatFormatting.GRAY))
                        .append(net.minecraft.network.chat.Component.literal(String.format("%.1f", health))
                                .withStyle(ChatFormatting.WHITE))
                        .append(net.minecraft.network.chat.Component.literal("]")
                                .withStyle(ChatFormatting.GRAY));

                float fontSize = 10;
                float textWidth = FontManager.get(fontSize).getComponentWidth(nameComponent);
                float textHeight = FontManager.get(fontSize).getHeight();

                float padding = 4f;

                float rectX = screenX - textWidth / 2f - padding;
                float rectY = screenY - textHeight - 8f;

                boolean isFriend = FriendRepo.isFriend(player);
                int bgColor = isFriend ? 0x8028FF28 : 0x90000000;

                RenderUtils.fillRoundRect(rectX, rectY,
                        textWidth + padding * 2f,
                        textHeight + 4f,
                        4f,
                        bgColor
                );

                FontManager.get(fontSize).drawComponent(
                        e.getGuiGraphics(),
                        nameComponent,
                        screenX - textWidth / 2f,
                        rectY + 2f,
                        0xFFFFFFFF
                );

                renderEquipment(e.getGuiGraphics(), player, screenX, rectY - 12f);
            }
        }
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
        if (stack != null && !stack.isEmpty()) {
            list.add(stack);
        }
    }

    private void renderItem(GuiGraphics g, ItemStack stack,
                            float x, float y, float size) {

        if (stack == null || stack.isEmpty()) return;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 16f, size / 16f);

        g.renderItem(stack, 0, 0);
        g.renderItemDecorations(mc.font, stack, 0, 0);

        g.pose().popMatrix();
    }

    private void renderWorldItems(GuiGraphics g) {

        for (ItemEntity item : mc.level.getEntitiesOfClass(
                ItemEntity.class,
                mc.player.getBoundingBox().inflate(64)
        )) {

            Vec3 screen = ProjectUtils.worldSpaceToScreenSpace(
                    new Vec3(item.getX(), item.getY() + 0.2, item.getZ())
            );

            if (screen == null || screen.z < 0 || screen.z > 1) continue;

            float x = (float) screen.x;
            float y = (float) screen.y;

            Component itemName = item.getItem().getHoverName();

            float fontSize = 10;
            float textWidth = FontManager.get(fontSize).getComponentWidth(itemName);

            float rectW = textWidth + 24;
            float rectH = 16;

            RenderUtils.fillRoundRect(
                    x - rectW / 2f,
                    y - rectH / 2f,
                    rectW,
                    rectH,
                    4f,
                    0x90000000
            );

            g.pose().pushMatrix();
            g.pose().translate(x - rectW / 2f + 4, y - 8);
            g.pose().scale(0.8f, 0.8f);
            g.renderItem(item.getItem(), 0, 0);
            g.renderItemDecorations(mc.font, item.getItem(), 0, 0);
            g.pose().popMatrix();

            FontManager.get(fontSize).drawComponent(
                    g,
                    itemName,
                    x - rectW / 2f + 20,
                    y - 6,
                    0xFFFFFFFF
            );
        }
    }
}