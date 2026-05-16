package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.awt.Color;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.misc.Core;
import ru.arixcompany.features.repos.SoundRepo;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

public abstract class AbstractSliderButton extends AbstractWidget.WithInactiveMessage {
    protected double value;
    protected boolean canChangeValue;
    private boolean dragging;

    public AbstractSliderButton(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message);
        this.value = value;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        Core module = Arix.getInstance().getModuleRepo().getModule(Core.class);

        if (module != null && module.isState() && module.customButtons.isValue()) {
            renderCustomSlider(g);
        } else {
            renderVanillaSlider(g);
        }

        if (this.isHovered()) {
            g.requestCursor(this.dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
        }
    }

    private void renderCustomSlider(GuiGraphics g) {
        Color theme = Arix.getInstance().getCurrentTheme().getMain();
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();
        float radius = 4f;

        int bgColor, outlineColor;
        if (!this.active) {
            bgColor = new Color(10, 10, 10, 80).getRGB();
            outlineColor = new Color(60, 60, 60, 40).getRGB();
        } else if (this.isHoveredOrFocused()) {
            bgColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 35).getRGB();
            outlineColor = theme.getRGB();
        } else {
            bgColor = new Color(15, 15, 15, 160).getRGB();
            outlineColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 120).getRGB();
        }

        RenderUtils.fillRoundRect(x, y, w, h, radius, bgColor);
        RenderUtils.drawRoundRectOutline(x, y, w, h, radius, 1.0f, outlineColor);

        float progressWidth = (float) (this.value * w);
        if (progressWidth > 4) {
            int progressColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 90).getRGB();
            RenderUtils.fillRoundRect(x, y, progressWidth, h, radius, progressColor);

            RenderUtils.fillRect(x + progressWidth - 1.5f, y + 2, 1.5f, h - 4, theme.getRGB());
        }

        float fontSize = 10f;
        Component label = this.getMessage();
        float tw = FontManager.get(fontSize).getComponentWidth(label);
        float th = FontManager.get(fontSize).getHeight();

        FontManager.get(fontSize).drawComponent(g, label,
                x + (w - tw) / 2f,
                y + (h - th) / 2f,
                this.active ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void renderVanillaSlider(GuiGraphics g) {
        Identifier sliderSprite = this.isActive() && this.isFocused() && !this.canChangeValue ?
                Identifier.withDefaultNamespace("widget/slider_highlighted") : Identifier.withDefaultNamespace("widget/slider");
        Identifier handleSprite = !this.isActive() || !this.isHovered && !this.canChangeValue ?
                Identifier.withDefaultNamespace("widget/slider_handle") : Identifier.withDefaultNamespace("widget/slider_handle_highlighted");

        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sliderSprite, getX(), getY(), getWidth(), getHeight(), net.minecraft.util.ARGB.white(this.alpha));
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, handleSprite, getX() + (int)(this.value * (this.width - 8)), getY(), 8, getHeight(), net.minecraft.util.ARGB.white(this.alpha));
        this.renderScrollingStringOverContents(g.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), this.getMessage(), 2);
    }

    @Override
    public void playDownSound(SoundManager manager) {
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.dragging = false;
        Core module = Arix.getInstance().getModuleRepo().getModule(Core.class);
        if (module != null && module.isState() && module.customButtons.isValue() && module.buttonSounds.isValue()) {
            SoundRepo.playButton();
        } else {
            super.playDownSound(Minecraft.getInstance().getSoundManager());
        }
    }

    @Override
    public void onClick(MouseButtonEvent p_424503_, boolean p_424772_) {
        this.dragging = this.active;
        this.setValueFromMouse(p_424503_);
    }

    private void setValueFromMouse(MouseButtonEvent event) {
        this.setValue((event.x() - (this.getX() + 4)) / (this.width - 8));
    }

    protected void setValue(double p_93612_) {
        double d0 = this.value;
        this.value = Mth.clamp(p_93612_, 0.0, 1.0);
        if (d0 != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double p_93591_, double p_93592_) {
        this.setValueFromMouse(event);
        super.onDrag(event, p_93591_, p_93592_);
    }

    @Override
    public void setFocused(boolean p_265705_) {
        super.setFocused(p_265705_);
        if (!p_265705_) {
            this.canChangeValue = false;
        } else {
            InputType inputtype = Minecraft.getInstance().getLastInputType();
            if (inputtype == InputType.MOUSE || inputtype == InputType.KEYBOARD_TAB) {
                this.canChangeValue = true;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent p_427303_) {
        if (p_427303_.isSelection()) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        }
        if (this.canChangeValue) {
            boolean flag = p_427303_.isLeft();
            boolean flag1 = p_427303_.isRight();
            if (flag || flag1) {
                float f = flag ? -1.0F : 1.0F;
                this.setValue(this.value + f / (this.width - 8));
                return true;
            }
        }
        return false;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.slider", this.getMessage());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_168798_) {
        p_168798_.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                p_168798_.add(NarratedElementType.USAGE, Component.translatable(this.canChangeValue ? "narration.slider.usage.focused" : "narration.slider.usage.focused.keyboard_cannot_change_value"));
            } else {
                p_168798_.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
            }
        }
    }

    protected abstract void updateMessage();
    protected abstract void applyValue();
}