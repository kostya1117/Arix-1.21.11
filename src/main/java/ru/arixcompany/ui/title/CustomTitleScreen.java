package ru.arixcompany.ui.title;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.arixcompany.ui.title.alt.AltManagerScreen;
import ru.arixcompany.ui.title.button.AbstractButton;
import ru.arixcompany.ui.title.button.implement.CustomTitleButton;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.font.FontManager;
import ru.arixcompany.utils.render.shader.shaders.FractalFlameShader;

import java.util.*;

public class CustomTitleScreen extends Screen implements IMinecraft {
    private final List<AbstractButton> buttons = new ArrayList<>();

    private final AbstractButton singleplayer = new CustomTitleButton("Одиночная игра", () -> mc.setScreen(new SelectWorldScreen(this)));
    private final AbstractButton multiplayer = new CustomTitleButton("Сетевая игра", () -> mc.setScreen(new JoinMultiplayerScreen(this)));
    private final AbstractButton accounts = new CustomTitleButton("Аккаунты", () -> mc.setScreen(new AltManagerScreen(this)));
    private final AbstractButton options = new CustomTitleButton("Настройки", () -> mc.setScreen(new OptionsScreen(this, this.minecraft.options)));
    private final AbstractButton exit = new CustomTitleButton("Выход", mc::stop);

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private static final int SMALL_BUTTON_WIDTH = 78;
    private static final int SMALL_BUTTON_HEIGHT = 20;
    private static final int SMALL_BUTTON_GAP = 4;
    private static final int SMALL_ROW_GAP = 8;

    public CustomTitleScreen() {
        super(Component.empty());
        buttons.addAll(Arrays.asList(singleplayer, multiplayer, accounts, options, exit));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();

        FractalFlameShader.draw(context, 0, 0, screenW, screenH);
        context.nextStratum();

        float centerX = screenW / 2f;
        float centerY = screenH / 2f;

        float mainButtonsHeight = BUTTON_HEIGHT * 3 + BUTTON_GAP * 2;
        float totalHeight = mainButtonsHeight + SMALL_ROW_GAP + SMALL_BUTTON_HEIGHT;

        float titleHeight = FontManager.get(24).getHeight("Arix");
        float titleGap = 16;

        float fullHeight = titleHeight + titleGap + totalHeight;
        float blockTop = centerY - fullHeight / 2f;

        float titleWidth = FontManager.get(24).getWidth("Arix");
        float titleX = centerX - titleWidth / 2f;
        FontManager.get(24).drawString(context, "Arix", titleX, blockTop, -1);

        float buttonTop = blockTop + titleHeight + titleGap;
        float buttonX = centerX - BUTTON_WIDTH / 2f;

        singleplayer.position((int) buttonX, (int) buttonTop)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT);

        multiplayer.position((int) buttonX, (int) (buttonTop + BUTTON_HEIGHT + BUTTON_GAP))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT);

        accounts.position((int) buttonX, (int) (buttonTop + (BUTTON_HEIGHT + BUTTON_GAP) * 2))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT);

        float smallRowY = buttonTop + mainButtonsHeight + SMALL_ROW_GAP;
        float smallTotalWidth = SMALL_BUTTON_WIDTH * 2 + SMALL_BUTTON_GAP;
        float smallStartX = centerX - smallTotalWidth / 2f;

        options.position((int) smallStartX, (int) smallRowY)
                .size(SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT);

        exit.position((int) (smallStartX + SMALL_BUTTON_WIDTH + SMALL_BUTTON_GAP), (int) smallRowY)
                .size(SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT);

        buttons.forEach(button -> button.render(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean p_431348_) {
        buttons.forEach(button -> button.mouseClicked(click.x(), click.y(), click.button()));
        return super.mouseClicked(click, p_431348_);
    }
}