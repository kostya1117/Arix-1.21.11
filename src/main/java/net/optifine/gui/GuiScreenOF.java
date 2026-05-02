package net.optifine.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.optifine.util.GuiUtils;

public class GuiScreenOF extends Screen {
    protected Font fontRenderer = Minecraft.getInstance().font;
    protected boolean mousePressed = false;
    protected Options settings = Minecraft.getInstance().options;

    public GuiScreenOF(Component title) {
        super(title);
    }

    public List<AbstractWidget> getButtonList() {
        List<AbstractWidget> list = new ArrayList<>();

        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget) {
                list.add((AbstractWidget)guieventlistener);
            }
        }

        return list;
    }

    protected void actionPerformed(AbstractWidget button) {
    }

    protected void actionPerformedRightClick(AbstractWidget button) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent eventIn, boolean doubleIn) {
        boolean flag = super.mouseClicked(eventIn, doubleIn);
        this.mousePressed = true;
        AbstractWidget abstractwidget = getSelectedButton((int)eventIn.x(), (int)eventIn.y(), this.getButtonList());
        if (abstractwidget != null && abstractwidget.active) {
            if (eventIn.button() == 1
                && abstractwidget instanceof IOptionControl ioptioncontrol
                && ioptioncontrol.getControlOption() == this.settings.GUI_SCALE) {
                abstractwidget.playDownSound(super.minecraft.getSoundManager());
            }

            if (eventIn.button() == 0) {
                this.actionPerformed(abstractwidget);
            } else if (eventIn.button() == 1) {
                this.actionPerformedRightClick(abstractwidget);
            }

            return true;
        } else {
            return flag;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        boolean flag = super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        AbstractWidget abstractwidget = getSelectedButton((int)mouseX, (int)mouseY, this.getButtonList());
        if (abstractwidget != null && abstractwidget.active && abstractwidget instanceof IOptionControl) {
            this.actionPerformed(abstractwidget);
            return true;
        } else {
            return flag;
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent eventIn) {
        if (!this.mousePressed) {
            return false;
        }

        this.mousePressed = false;
        this.setDragging(false);
        return this.getFocused() != null && this.getFocused().mouseReleased(eventIn) ? true : super.mouseReleased(eventIn);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent eventIn, double mouseX, double mouseY) {
        return !this.mousePressed ? false : super.mouseDragged(eventIn, mouseX, mouseY);
    }

    public static AbstractWidget getSelectedButton(int x, int y, List<AbstractWidget> listButtons) {
        for (int i = 0; i < listButtons.size(); i++) {
            AbstractWidget abstractwidget = listButtons.get(i);
            if (abstractwidget.visible) {
                int j = GuiUtils.getWidth(abstractwidget);
                int k = GuiUtils.getHeight(abstractwidget);
                if (x >= abstractwidget.getX()
                    && y >= abstractwidget.getY()
                    && x < abstractwidget.getX() + j
                    && y < abstractwidget.getY() + k) {
                    return abstractwidget;
                }
            }
        }

        return null;
    }

    public static void drawString(GuiGraphics graphicsIn, Font fontRendererIn, String textIn, int xIn, int yIn, int colorIn) {
        graphicsIn.drawCenteredString(fontRendererIn, textIn, xIn, yIn, colorIn);
    }

    public static void drawCenteredString(GuiGraphics graphicsIn, Font fontRendererIn, FormattedCharSequence textIn, int xIn, int yIn, int colorIn) {
        graphicsIn.drawCenteredString(fontRendererIn, textIn, xIn, yIn, colorIn);
    }

    public static void drawCenteredString(GuiGraphics graphicsIn, Font fontRendererIn, String textIn, int xIn, int yIn, int colorIn) {
        graphicsIn.drawCenteredString(fontRendererIn, textIn, xIn, yIn, colorIn);
    }

    public static void drawCenteredString(GuiGraphics graphicsIn, Font fontRendererIn, Component textIn, int xIn, int yIn, int colorIn) {
        graphicsIn.drawCenteredString(fontRendererIn, textIn, xIn, yIn, colorIn);
    }

    public static boolean hasShiftDown() {
        return Minecraft.getInstance().hasShiftDown();
    }

    public static boolean hasAltDown() {
        return Minecraft.getInstance().hasAltDown();
    }
}
