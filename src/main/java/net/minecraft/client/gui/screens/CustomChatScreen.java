package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AnimatedEditBox;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.ui.draggable.draggables.ChatDraggable;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

public class CustomChatScreen extends ChatScreen {

    private static final float FONT_SIZE = 12f;

    private final CustomFont customFont;

    public CustomChatScreen(String initial, boolean isDraft) {
        super(initial, isDraft);
        this.customFont = FontManager.get(FONT_SIZE);
    }

    @Override
    protected void init() {
        super.init();

        if (this.input instanceof AnimatedEditBox animated) {
            animated.setCustomFont(customFont);
        }
        this.input.setY(this.height - 14);
        this.commandSuggestions.setCustomFont(customFont);
    }

    @Override
    protected boolean shouldSkipChatRender() {
        return ChatDraggable.isCustomChatActive();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int bgColor = this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE);

        float bgX = this.input.getX() - 3f;
        float bgY = this.input.getY() + (this.input.getHeight() - 14f) / 2f;
        float bgW = this.input.getWidth() + 6f;
        float bgH = 14f;

        Interface.drawClientRect(bgX, bgY, bgW, bgH, 5f, bgColor);

        if (!shouldSkipChatRender()) {
            this.minecraft.gui.getChat().render(
                    g, this.font,
                    this.minecraft.gui.getGuiTicks(),
                    mouseX, mouseY, true,
                    insertionClickMode()
            );
        }

        this.renderBackground(g, mouseX, mouseY, delta);
        for (int i = 0; i < this.renderables.size(); i++) {
            this.renderables.get(i).render(g, mouseX, mouseY, delta);
        }

        this.commandSuggestions.render(g, mouseX, mouseY);
        renderDraggables(g, mouseX, mouseY, delta);
    }
}
