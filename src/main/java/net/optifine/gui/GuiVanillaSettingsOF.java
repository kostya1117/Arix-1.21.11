package net.optifine.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiVanillaSettingsOF extends GuiScreenOF {
    private Screen prevScreen;
    private Options settings;
    private final int oldAnisotropyBit;
    private final TextureFilteringMethod oldTextureFiltering;
    private AbstractWidget buttonMaxAnisotropyBit;
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiVanillaSettingsOF(Screen guiscreen, Options optionsIn) {
        super(Component.translatable("of.options.vanillaTitle"));
        this.prevScreen = guiscreen;
        this.settings = optionsIn;
        this.oldAnisotropyBit = optionsIn.maxAnisotropyBit().get();
        this.oldTextureFiltering = optionsIn.textureFiltering().get();
    }

    @Override
    public void init() {
        this.clearWidgets();
        OptionInstance[] aoptioninstance = new OptionInstance[]{this.settings.textureFiltering(), this.settings.maxAnisotropyBit(), this.settings.improvedTransparency()};

        for (int i = 0; i < aoptioninstance.length; i++) {
            OptionInstance optioninstance = aoptioninstance[i];
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 + 21 * (i / 2) - 12;
            k += 21;
            AbstractWidget abstractwidget = this.addRenderableWidget(optioninstance.createButton(this.minecraft.options, j, k, 150));
            if (optioninstance == this.settings.maxAnisotropyBit()) {
                this.buttonMaxAnisotropyBit = abstractwidget;
            }

            abstractwidget.setTooltip(null);
        }

        this.addRenderableWidget(new GuiButtonOF(200, this.width / 2 - 100, this.height / 6 + 168 + 11, I18n.get("gui.done")));
    }

    @Override
    protected void actionPerformed(AbstractWidget guiElement) {
        if (guiElement instanceof GuiButtonOF guibuttonof) {
            if (guibuttonof.active) {
                if (guibuttonof.id == 200) {
                    this.minecraft.options.save();
                    this.minecraft.setScreen(this.prevScreen);
                }

                this.minecraft.resizeDisplay();
            }
        }
    }

    @Override
    public void removed() {
        this.minecraft.options.save();
        if (this.settings.maxAnisotropyBit().get() != this.oldAnisotropyBit || this.settings.textureFiltering().get() != this.oldTextureFiltering) {
            this.minecraft.updateMaxMipLevel(this.settings.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }

        super.removed();
    }

    @Override
    public void tick() {
        if (this.buttonMaxAnisotropyBit != null) {
            this.buttonMaxAnisotropyBit.active = this.settings.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC;
        }

        super.tick();
    }

    @Override
    public void render(GuiGraphics graphicsIn, int x, int y, float partialTicks) {
        super.render(graphicsIn, x, y, partialTicks);
        drawCenteredString(graphicsIn, this.minecraft.font, this.title, this.width / 2, 15, -1);
        this.tooltipManager.drawTooltips(graphicsIn, x, y, this.getButtonList());
    }
}
