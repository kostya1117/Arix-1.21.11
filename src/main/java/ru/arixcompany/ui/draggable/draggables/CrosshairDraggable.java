package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.RenderUtils;

public class CrosshairDraggable extends DraggableComponent {

    private float circleAnimation = -360.0f;
    private static final float CIRCLE_RADIUS = 3.5f;
    private static final float CIRCLE_THICKNESS = 1.0f;

    private float spreadAnimation = 0.0f;
    private static final float LINE_LENGTH = 4.0f;
    private static final float LINE_THICKNESS = 2.0f;
    private static final float LINE_GAP = 3.0f;
    private static final float LINE_SPREAD = 2.0f;

    private float lastYaw;
    private float lastPitch;
    private float animatedYaw;
    private float animatedPitch;
    private float animationSize;

    private static final float ORB_BASE_RADIUS = 1.0f;
    private static final float ORB_MOVE_SIZE = 5.0f;

    public SelectSetting mode = new SelectSetting("Режим")
            .value("Круг", "Орбиз", "Прицел");

    public BooleanSetting staticCrosshair = new BooleanSetting("Статический")
            .visible(() -> mode.isSelected("Орбиз"));

    public BooleanSetting dot = new BooleanSetting("Точка")
            .visible(() -> mode.isSelected("Прицел"));

    public CrosshairDraggable() {
        super("Crosshair", 0, 0, 20, 20);
        setPinned(true);
        setup(mode, staticCrosshair, dot);
    }

    @Override
    protected void updateVisibility() {
        this.visible = isCustomCrosshairActive();
    }

    public static boolean isCustomCrosshairActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Crosshair");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.player == null) return;
        if (mc.options == null || !mc.options.getCameraType().isFirstPerson()) return;

        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f;

        if (mode.isSelected("Орбиз")) {
            renderOrbizMode(centerX, centerY, alpha);
        } else if (mode.isSelected("Круг")) {
            renderCircleMode(centerX, centerY, alpha);
        } else {
            renderCrosshairMode(centerX, centerY, alpha);
        }

        float totalSize = 36.0f;
        this.width = totalSize;
        this.height = totalSize;
        this.x = centerX - totalSize / 2.0f;
        this.y = centerY - totalSize / 2.0f;
        this.renderX = this.x;
        this.renderY = this.y;
    }

    private void renderCircleMode(float cx, float cy, float alpha) {
        float cooldown = mc.player.getAttackStrengthScale(1.0f);
        float targetAngle = Mth.clamp(cooldown * 360.0f, 0.0f, 360.0f);

        this.circleAnimation += (-targetAngle - this.circleAnimation) / 4.0f;

        int bgColor = argb(30, 30, 30, alpha);
        int accentColor = Colors.accent(alpha);

        RenderUtils.drawCircle(cx, cy, 0.0f, 360.0f, CIRCLE_RADIUS, CIRCLE_THICKNESS, bgColor);

        if (Math.abs(circleAnimation) > 0.5f) {
            RenderUtils.drawCircle(cx, cy, 0.0f, circleAnimation, CIRCLE_RADIUS, CIRCLE_THICKNESS, accentColor);
        }
    }

    private void renderOrbizMode(float cx, float cy, float alpha) {
        float x = cx;
        float y = cy;

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float strafe = 0.0f;
        float forward = 0.0f;

        if (mc.player.input != null) {
            strafe = mc.player.input.leftImpulse;
            forward = mc.player.input.forwardImpulse;
        }

        animatedYaw = MathUtils.fast(animatedYaw, ((lastYaw - currentYaw) + strafe) * ORB_MOVE_SIZE, 5.0f);
        animatedPitch = MathUtils.fast(animatedPitch, ((lastPitch - currentPitch) + forward) * ORB_MOVE_SIZE, 5.0f);

        if (!staticCrosshair.isValue()) {
            x += animatedYaw;
            y += animatedPitch;
        }

        animationSize = MathUtils.fast(
                animationSize,
                (1.0f - mc.player.getAttackStrengthScale(1.0f)) * 3.0f,
                10.0f
        );

        float radius = ORB_BASE_RADIUS + (staticCrosshair.isValue() ? 0.0f : animationSize);

        int orbColor = Colors.accent(alpha);

        RenderUtils.drawCircle(
                x,
                y,
                0.0f,
                360.0f,
                radius,
                1F,
                orbColor
        );

        lastYaw = currentYaw;
        lastPitch = currentPitch;
    }

    private void renderCrosshairMode(float cx, float cy, float alpha) {
        float cooldown = mc.player.getAttackStrengthScale(1.0f);

        float targetSpread = (1.0f - cooldown) * LINE_SPREAD;
        this.spreadAnimation += (targetSpread - this.spreadAnimation) / 4.0f;

        float gap = LINE_GAP + spreadAnimation;

        int bgColor = argb(30, 30, 30, alpha * 0.6f);
        int accentColor = Colors.accent(alpha);

        float shadowOff = 0.5f;
        float shadowThick = LINE_THICKNESS + 1.0f;

        RenderUtils.fillRoundRect(cx - shadowThick / 2f + shadowOff, cy - gap - LINE_LENGTH + shadowOff,
                shadowThick, LINE_LENGTH, 1.0f, bgColor);
        RenderUtils.fillRoundRect(cx - LINE_THICKNESS / 2f, cy - gap - LINE_LENGTH,
                LINE_THICKNESS, LINE_LENGTH, 1.0f, accentColor);

        RenderUtils.fillRoundRect(cx - shadowThick / 2f + shadowOff, cy + gap + shadowOff,
                shadowThick, LINE_LENGTH, 1.0f, bgColor);
        RenderUtils.fillRoundRect(cx - LINE_THICKNESS / 2f, cy + gap,
                LINE_THICKNESS, LINE_LENGTH, 1.0f, accentColor);

        RenderUtils.fillRoundRect(cx - gap - LINE_LENGTH + shadowOff, cy - shadowThick / 2f + shadowOff,
                LINE_LENGTH, shadowThick, 1.0f, bgColor);
        RenderUtils.fillRoundRect(cx - gap - LINE_LENGTH, cy - LINE_THICKNESS / 2f,
                LINE_LENGTH, LINE_THICKNESS, 1.0f, accentColor);

        RenderUtils.fillRoundRect(cx + gap + shadowOff, cy - shadowThick / 2f + shadowOff,
                LINE_LENGTH, shadowThick, 1.0f, bgColor);
        RenderUtils.fillRoundRect(cx + gap, cy - LINE_THICKNESS / 2f,
                LINE_LENGTH, LINE_THICKNESS, 1.0f, accentColor);

        if (dot.isValue()) {
            float dotSize = 1.5f;
            RenderUtils.fillRoundRect(
                    cx - dotSize / 2f,
                    cy - dotSize / 2f,
                    dotSize,
                    dotSize,
                    dotSize / 2f,
                    accentColor
            );
        }
    }

    private int argb(int r, int g, int b, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0f, 1f) * 255f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected boolean isDragHandle(double mouseX, double mouseY) {
        float cx = this.renderX + this.width / 2.0f;
        float cy = this.renderY + this.height / 2.0f;
        float half = 18.0f;

        return mouseX >= cx - half && mouseX <= cx + half
                && mouseY >= cy - half && mouseY <= cy + half;
    }
}