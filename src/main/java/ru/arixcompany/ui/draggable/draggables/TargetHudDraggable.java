package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.ProjectUtils;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.List;
import static ru.arixcompany.utils.render.ColorUtil.argb;

public class TargetHudDraggable extends DraggableComponent {

    private static final float PAD = 4.0f;
    private static final float HEAD_SIZE = 18.0f;
    private static final float BAR_HEIGHT = 2.0f;
    private static final float BAR_PAD = 4.0f;
    private static final float ITEM_SIZE = 12.0f;
    private static final float ITEM_GAP = 1.5f;
    private static final float GAP = 1.0f;
    private static final float PANEL_RADIUS = 6.0f;
    private static final float NAME_FONT = 8.0f;
    private static final float HP_FONT = 7.0f;

    private float healthAnimation = 0.0f;
    private LivingEntity lastTarget = null;
    private float openAnim = 0.0f;

    private float projected3dX = 0;
    private float projected3dY = 0;
    private boolean has3dPos = false;

    private float settingsAnchorX = 0;
    private float settingsAnchorY = 0;

    public BooleanSetting render3d = new BooleanSetting("3D на таргете");
    public SelectSetting pos3d = new SelectSetting("3D позиция")
            .value("Над головой", "По центру")
            .visible(() -> render3d.isValue());

    public SelectSetting hpMode = new SelectSetting("Отображение HP")
            .value("Проценты", "Значение", "Оба");

    public TargetHudDraggable() {
        super("TargetHUD", 0, 0, 100, 36);
        setup(render3d,pos3d, hpMode);
        EventRepo.register(this);
        if (mc.getWindow() != null) {
            this.x = mc.getWindow().getGuiScaledWidth() / 2f - 50;
            this.y = mc.getWindow().getGuiScaledHeight() / 2f + 30;
            this.renderX = this.x;
            this.renderY = this.y;
        }
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("TargetHUD");
    }

    @EventHandler
    public void update3dPosition(EventRender3D e) {
        float tickDelta = e.getTickDelta();
        has3dPos = false;
        if (!render3d.isValue()) return;

        LivingEntity target = resolveTarget();
        if (target == null || mc.level == null) return;

        double x = Mth.lerp(tickDelta, target.xo, target.getX());
        double y = Mth.lerp(tickDelta, target.yo, target.getY());
        double z = Mth.lerp(tickDelta, target.zo, target.getZ());

        double projY;
        if (pos3d.isSelected("По центру")) {
            projY = y + target.getBbHeight() / 2.0;
        } else {
            projY = y + target.getBbHeight() + 0.5;
        }

        Vec3 screen = ProjectUtils.worldSpaceToScreenSpace(new Vec3(x, projY, z));

        if (screen != null && screen.z > 0 && screen.z < 1) {
            projected3dX = (float) screen.x;
            projected3dY = (float) screen.y;
            has3dPos = true;
        }
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        LivingEntity target = resolveTarget();

        float targetOpen = target != null ? 1.0f : 0.0f;
        openAnim = MathUtils.fast(openAnim, targetOpen, 6.0f);

        if (openAnim < 0.01f) {
            healthAnimation = 0.0f;
            lastTarget = null;
            return;
        }

        if (target == null) return;

        if (target != lastTarget) {
            healthAnimation = target.getHealth() / target.getMaxHealth();
            lastTarget = target;
        }

        float currentHp = target.getHealth();
        float maxHp = target.getMaxHealth();
        healthAnimation = Mth.clamp(
                MathUtils.fast(healthAnimation, currentHp / maxHp, 10.0f),
                0.0f, 1.0f
        );

        CustomFont nameFont = FontManager.get(NAME_FONT);
        CustomFont hpFont = FontManager.get(HP_FONT);

        String name = target.getName().getString();
        String hpText = buildHpText(currentHp, maxHp);

        float nameW = nameFont.getWidth(name);
        float hpTextW = hpFont.getWidth(hpText);

        List<ItemStack> items = collectItems(target);
        float itemsRowW = items.isEmpty() ? 0 : items.size() * (ITEM_SIZE + ITEM_GAP) - ITEM_GAP;

        float nameLineW = nameW + GAP * 4 + hpTextW;
        float contentW = Math.max(nameLineW, itemsRowW);

        float totalW = PAD + HEAD_SIZE + PAD + contentW + PAD;

        float contentH = nameFont.getHeight() + (items.isEmpty() ? 0 : GAP + ITEM_SIZE);
        float innerH = Math.max(HEAD_SIZE, contentH);
        float totalH = PAD + innerH + PAD + BAR_HEIGHT + BAR_PAD;

        this.width = totalW;
        this.height = totalH;

        boolean isPreview = target == mc.player;
        boolean use3d = render3d.isValue() && !isPreview;

        float drawX, drawY;
        if (use3d) {
            if (!has3dPos) return;

            drawX = projected3dX - totalW / 2.0f;

            if (pos3d.isSelected("По центру")) {
                drawY = projected3dY - totalH / 2.0f;
            } else {
                drawY = projected3dY - totalH - 4;
            }
        } else {
            drawX = rx;
            drawY = ry;
        }

        settingsAnchorX = drawX;
        settingsAnchorY = drawY;

        float anim = openAnim * alpha;

        RenderUtils.fillRoundRect(drawX, drawY, totalW, totalH, PANEL_RADIUS,
                Colors.bgPrimary(anim * 0.92f));

        float headX = drawX + PAD;
        float headY = drawY + PAD + (innerH - HEAD_SIZE) / 2.0f;

        if (target instanceof Player player) {
            renderPlayerHead(graphics, player, headX, headY, HEAD_SIZE);
        } else {
            RenderUtils.fillRoundRect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4,
                    Colors.bgElement(anim));
        }

        float cx = headX + HEAD_SIZE + PAD;
        float topY = drawY + PAD + (innerH - contentH) / 2.0f;

        boolean isFriend = target instanceof Player p && FriendRepo.isFriend(p);
        int nameColor = isFriend ? Colors.friend(anim) : Colors.textActive(anim);
        nameFont.drawString(graphics, name, cx, topY, nameColor);

        float hpX = cx + nameW + GAP * 4;
        hpFont.drawString(graphics, hpText, hpX,
                topY + (nameFont.getHeight() - hpFont.getHeight()) / 2f,
                Colors.textInactive(anim * 0.6f));

        if (!items.isEmpty()) {
            float itemsY = topY + nameFont.getHeight() + GAP;
            for (int i = 0; i < items.size(); i++) {
                float ix = cx + i * (ITEM_SIZE + ITEM_GAP);

                RenderUtils.fillRoundRect(ix, itemsY, ITEM_SIZE, ITEM_SIZE, 2,
                        Colors.bgElement(anim * 0.4f));

                graphics.pose().pushMatrix();
                graphics.pose().translate(ix, itemsY);
                float scale = ITEM_SIZE / 16.0f;
                graphics.pose().scale(scale, scale);
                graphics.renderItem(items.get(i), 0, 0);
                graphics.renderItemDecorations(mc.font, items.get(i), 0, 0);
                graphics.pose().popMatrix();
            }
        }

        float barY = drawY + totalH - BAR_HEIGHT - BAR_PAD;
        float barX = drawX + BAR_PAD;
        float barW = totalW - BAR_PAD * 2;

        RenderUtils.fillRoundRect(barX, barY, barW, BAR_HEIGHT, BAR_HEIGHT / 2f,
                Colors.bgElement(anim * 0.4f));

        if (healthAnimation > 0) {
            float fillW = barW * healthAnimation;
            int accentColor = Colors.accent(anim);
            RenderUtils.horizontalGradient(barX, barY, fillW, BAR_HEIGHT,
                    BAR_HEIGHT / 2f, accentColor, brighten(accentColor, 0.3f));
        }

        float abs = target.getAbsorptionAmount();
        if (abs > 0) {
            float absW = Math.min(abs / maxHp, 1.0f) * barW;
            int absColor = argb(255, 200, 0, anim * 0.7f);
            RenderUtils.fillRoundRect(barX, barY, absW, BAR_HEIGHT, BAR_HEIGHT / 2f, absColor);
        }
    }

    private String buildHpText(float currentHp, float maxHp) {
        if (hpMode.isSelected("Проценты")) {
            return (int) (healthAnimation * 100) + "%";
        } else if (hpMode.isSelected("Значение")) {
            return String.format("%.1f", currentHp) + "/" + String.format("%.0f", maxHp);
        } else {
            return String.format("%.1f", currentHp) + " (" + (int) (healthAnimation * 100) + "%)";
        }
    }

    private LivingEntity resolveTarget() {
        HitAura ka = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        if (ka != null && ka.isState() && ka.getTarget() instanceof LivingEntity le) {
            return le;
        }

        if (mc.screen instanceof ChatScreen && mc.player != null) {
            return mc.player;
        }

        return null;
    }

    private List<ItemStack> collectItems(LivingEntity target) {
        List<ItemStack> items = new ArrayList<>();
        if (!(target instanceof Player player)) return items;

        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) items.add(stack);
        }

        return items;
    }

    private void renderPlayerHead(GuiGraphics graphics, Player player, float x, float y, float size) {
        PlayerInfo info = mc.getConnection() != null
                ? mc.getConnection().getPlayerInfo(player.getUUID())
                : null;

        if (info == null) {
            RenderUtils.fillRoundRect(x, y, size, size, 4, Colors.bgElement(1.0f));
            return;
        }

        PlayerSkin skin = info.getSkin();
        Identifier texture = skin.body().id();

        RenderUtils.fillRoundRect(x, y, size, size, 4, Colors.bgElement(0.4f));

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                (int) x, (int) y, 8, 8, (int) size, (int) size, 8, 8, 64, 64);

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                (int) x, (int) y, 40, 8, (int) size, (int) size, 8, 8, 64, 64);
    }

    private int brighten(int color, float amount) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * (1.0f + amount)));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * (1.0f + amount)));
        int b = Math.min(255, (int) ((color & 0xFF) * (1.0f + amount)));
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected boolean isDragHandle(double mouseX, double mouseY) {
        LivingEntity target = resolveTarget();
        if (render3d.isValue() && target != null && target != mc.player) {
            return false;
        }
        return isMouseOver(mouseX, mouseY);
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= settingsAnchorX && mouseX <= settingsAnchorX + width
                && mouseY >= settingsAnchorY && mouseY <= settingsAnchorY + height;
    }
}