package ru.arixcompany.features.module.modules.render;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CrosshairArrows extends Module {

    private final ValueSetting arrowSize = new ValueSetting("Размер")
            .setValue(24).range(8, 48).step(1);

    private final BooleanSetting showDistance = new BooleanSetting("Расстояние")
            .setValue(true);

    private final BooleanSetting onlyFriends = new BooleanSetting("Только друзья")
            .setValue(false);

    private final List<ArrowEntry> entries = new ArrayList<>();

    public CrosshairArrows() {
        super("CrosshairArrows", Category.Render);
        setup(arrowSize, showDistance, onlyFriends);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        entries.clear();
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.level == null) return;

        float cx = mc.getWindow().getGuiScaledWidth()  / 2f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2f;

        List<Player> players = new ArrayList<>(mc.level.players());
        players.remove(mc.player);

        for (Player p : players) {
            boolean exists = false;
            for (ArrowEntry en : entries) {
                if (en.player == p) { exists = true; break; }
            }
            if (!exists) entries.add(new ArrowEntry(p));
        }

        for (ArrowEntry en : entries) {
            en.update(players);
        }

        float sz = arrowSize.getValue();

        for (ArrowEntry en : entries) {
            float anim = en.animation.getOutput();
            if (anim <= 0.001f) continue;

            en.render(e, cx, cy, sz);
        }

        entries.removeIf(en ->
                en.animation.getDirection() == Direction.BACKWARDS
                        && en.animation.getOutput() == 0f);
    }

    private static float fast(float current, float target, float speed) {
        return current + (target - current) / speed;
    }

    private class ArrowEntry {
        final Player player;
        final Animation animation = new EaseInOutQuad(300, 1.0);
        float smoothYaw;

        ArrowEntry(Player player) {
            this.player = player;
        }

        void update(List<Player> alivePlayers) {
            boolean alive = alivePlayers.contains(player) && player.isAlive();
            boolean isTarget = player == HitAura.getTarget();
            boolean isFriend = FriendRepo.isFriend(player);

            boolean shouldShow = alive && !isTarget;

            if (onlyFriends.isValue()) {
                shouldShow = shouldShow && isFriend;
            }

            animation.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);
        }

        void render(EventRender2D e, float cx, float cy, float sz) {
            float anim = animation.getOutput();

            float realYaw = RotationManager.getInstance().isRotating() && RotationManager.playerRotation != null
                    ? RotationManager.playerRotation.yaw()
                    : mc.gameRenderer.getMainCamera().yRot();
            smoothYaw = fast(smoothYaw, realYaw, 10f);

            float targetRadius = anim * 40f;
            if (mc.screen instanceof AbstractContainerScreen) targetRadius += 150f;
            else if (mc.screen instanceof InventoryScreen)    targetRadius += 130f;

            float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double ex = player.xo + (player.getX() - player.xo) * tickDelta
                    - mc.gameRenderer.getMainCamera().position().x;
            double ey = player.yo + (player.getY() - player.yo) * tickDelta
                    + player.getBbHeight() / 2.0
                    - mc.gameRenderer.getMainCamera().position().y
                    - mc.player.getEyeHeight();
            double ez = player.zo + (player.getZ() - player.zo) * tickDelta
                    - mc.gameRenderer.getMainCamera().position().z;

            double distance = Math.sqrt(ex * ex + ey * ey + ez * ez);

            double yawRad = smoothYaw * (Math.PI / 180.0);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double rotateYaw   = -(ez * cos - ex * sin);
            double rotatePitch = -(ex * cos + ez * sin);

            double angleDeg = Math.atan2(rotateYaw, rotatePitch) * 180.0 / Math.PI;
            double angleRad = Math.toRadians(angleDeg);

            double xPos = targetRadius * Math.cos(angleRad) + cx;
            double yPos = targetRadius * Math.sin(angleRad) + cy;

            int color;
            if (FriendRepo.isFriend(player)) {
                color = Colors.friend(anim);
            } else {
                Color main = Arix.getInstance().getCurrentTheme().getMain();
                color = new Color(main.getRed(), main.getGreen(), main.getBlue(),
                        (int)(anim * 255f)).getRGB();
            }

            Matrix3x2fStack pose = e.getGuiGraphics().pose();
            pose.pushMatrix();

            pose.translate((float) xPos, (float) yPos);
            pose.rotate((float)(angleRad - Math.PI / 2.0));
            float half = sz / 2f;
            RenderUtils.drawImage(e.getGuiGraphics(), Textures.arrow,
                    -half, -half, sz, sz, color);

            pose.popMatrix();

            if (showDistance.isValue()) {
                String distText = (int) distance + "м";
                float fontSize = 9f;
                float tw = FontManager.get(fontSize).getWidth(distText);
                float th = FontManager.get(fontSize).getHeight();

                float textDist = targetRadius + half + 4f;
                float tx = (float)(textDist * Math.cos(angleRad) + cx - tw / 2f);
                float ty = (float)(textDist * Math.sin(angleRad) + cy - th / 2f);

                FontManager.get(fontSize).drawString(
                        e.getGuiGraphics(), distText, tx, ty,
                        new Color(255, 255, 255, (int)(anim * 200f)).getRGB()
                );
            }
        }
    }
}