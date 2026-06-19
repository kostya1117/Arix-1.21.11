package net.minecraft.client.gui.components;

import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager; // Не забудь импорт
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.repos.SoundRepo;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

public abstract class AbstractButton extends AbstractWidget.WithInactiveMessage {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );
    private @Nullable Supplier<Boolean> overrideRenderHighlightedSprite;

    public AbstractButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public abstract void onPress(InputWithModifiers p_428560_);

    @Override
    public void playDownSound(SoundManager manager) {
        Interface module = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        if (module.isState() && module.customButtons.isValue() && module.buttonSounds.isValue()) {
            SoundRepo.playButton();
        } else {
            super.playDownSound(manager);
        }
    }

    @Override
    protected final void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        Interface module = Arix.getInstance().getModuleRepo().getModule(Interface.class);

        if (module.isState() && module.customButtons.isValue()) {
            renderCustomContents(g, mouseX, mouseY, partialTicks);
        } else {
            this.renderContents(g, mouseX, mouseY, partialTicks);
        }
        this.handleCursor(g);
    }

    protected abstract void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTicks);

    protected void renderCustomContents(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        Color theme = Arix.getInstance().getCurrentTheme().getMain();
        int bgColor, outlineColor, textColor;

        if (!this.active) {
            bgColor = new Color(10, 10, 10, 80).getRGB();
            outlineColor = new Color(60, 60, 60, 40).getRGB();
            textColor = 0xFFAAAAAA;
        } else if (this.isHoveredOrFocused()) {
            bgColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 45).getRGB();
            outlineColor = theme.getRGB();
            textColor = 0xFFFFFFFF;
        } else {
            bgColor = new Color(15, 15, 15, 160).getRGB();
            outlineColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 130).getRGB();
            textColor = 0xFFD0D0D0;
        }

        RenderUtils.fillRoundRect(g, getX(), getY(), getWidth(), getHeight(), 4f, bgColor);

        float fontSize = 10f;
        Component label = this.getMessage();
        float tw = FontManager.get(fontSize).getComponentWidth(label);
        float th = FontManager.get(fontSize).getHeight();

        FontManager.get(fontSize).drawComponent(g, label,
                getX() + (getWidth() - tw) / 2f,
                getY() + (getHeight() - th) / 2f,
                textColor);
    }

    protected void renderDefaultLabel(ActiveTextCollector collector) {
        this.renderScrollingStringOverContents(collector, this.getMessage(), 2);
    }

    protected final void renderDefaultSprite(GuiGraphics g) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,
                SPRITES.get(this.active, this.overrideRenderHighlightedSprite != null ? this.overrideRenderHighlightedSprite.get() : this.isHoveredOrFocused()),
                this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha)
        );
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean p_428686_) {
        this.onPress(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isActive()) return false;
        if (event.isSelection()) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress(event);
            return true;
        }
        return false;
    }
}