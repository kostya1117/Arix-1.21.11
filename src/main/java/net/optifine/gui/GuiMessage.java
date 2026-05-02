package net.optifine.gui;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.optifine.Config;

public class GuiMessage extends GuiScreenOF {
    private Screen parentScreen;
    private Component messageLine1;
    private Component messageLine2;
    private final List<FormattedCharSequence> listLines2 = Lists.newArrayList();
    protected String confirmButtonText;
    private int ticksUntilEnable;

    public GuiMessage(Screen parentScreen, String line1, String line2) {
        super(Component.translatable("of.options.detailsTitle"));
        this.parentScreen = parentScreen;
        this.messageLine1 = Component.literal(line1);
        this.messageLine2 = Component.literal(line2);
        this.confirmButtonText = I18n.get("gui.done", new Object[0]);
    }

    public void init() {
        this.addRenderableWidget(new GuiButtonOF(0, this.width / 2 - 100, this.height / 6 + 96, this.confirmButtonText));
        this.listLines2.clear();
        this.listLines2.addAll(this.minecraft.font.split(this.messageLine2, this.width - 50));
    }

    protected void actionPerformed(AbstractWidget button) {
        Config.getMinecraft().setScreen(this.parentScreen);
    }

    public void render(GuiGraphics graphicsIn, int mouseX, int mouseY, float partialTicks) {
        super.render(graphicsIn, mouseX, mouseY, partialTicks);
        drawCenteredString(graphicsIn, this.fontRenderer, this.messageLine1, this.width / 2, 70, -1);
        int var4 = 90;

        for(Iterator<FormattedCharSequence> var5 = this.listLines2.iterator(); var5.hasNext(); var4 += 9) {
            FormattedCharSequence line = (FormattedCharSequence)var5.next();
            drawCenteredString(graphicsIn, this.fontRenderer, line, this.width / 2, var4, -1);
            Objects.requireNonNull(this.fontRenderer);
        }

    }

    public void setButtonDelay(int ticksUntilEnable) {
        this.ticksUntilEnable = ticksUntilEnable;

        Button var3;
        for(Iterator var2 = this.getButtonList().iterator(); var2.hasNext(); var3.active = false) {
            var3 = (Button)var2.next();
        }

    }

    public void tick() {
        super.tick();
        Button var2;
        if (--this.ticksUntilEnable == 0) {
            for(Iterator var1 = this.getButtonList().iterator(); var1.hasNext(); var2.active = true) {
                var2 = (Button)var1.next();
            }
        }

    }
}
