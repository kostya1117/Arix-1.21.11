package ru.arixcompany.features.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.quad.EaseInOutQuad;

import java.awt.Color;
import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class ColorSetting extends Setting {
    private float current;
    private float minimum = 0.0F;
    private float maximum = 106.0F;
    private float increment = 1.0F;
    private float saturation = 1.0F;
    private float brightness = 1.0F;
    private float alpha = 1.0F;
    private Animation animation = new EaseInOutQuad(300, 1.0);
    private boolean open = false;
    private final Animation openAnim;

    public ColorSetting(String name, float current) {
        super(name);
        this.current = current;
        this.openAnim = new EaseInOutQuad(200, 1.0);
        this.openAnim.setDirection(Direction.BACKWARDS);
        this.openAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    public ColorSetting(String name, float current, float saturation, float brightness) {
        super(name);
        this.current = current;
        this.saturation = saturation;
        this.brightness = brightness;
        this.openAnim = new EaseInOutQuad(200, 1.0);
        this.openAnim.setDirection(Direction.BACKWARDS);
        this.openAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    public void toggle() {
        open = !open;
        openAnim.setDirection(open ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public boolean isOpen() {
        return open;
    }

    public ColorSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }

    public Color getColor() {
        float hue = this.current / this.maximum;
        return Color.getHSBColor(hue, this.saturation, this.brightness);
    }

    public void setColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.current = hsb[0] * this.maximum;
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    public float getHue() {
        return this.current / this.maximum;
    }

    public int getRGB() {
        return getColor().getRGB();
    }

    public int getRGBA(int alpha) {
        Color color = getColor();
        return alpha << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
    }
}