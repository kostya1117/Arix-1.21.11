package ru.arixcompany.clickgui.widgets;

import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.Colors;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@NoArgsConstructor
public final class SearchComponent implements IComponent {

    private static final float SEARCH_WIDTH  = 200.0F;
    private static final float SEARCH_HEIGHT = 18.0F;
    private static final float SEARCH_Y      = 8.0F;

    private float getSearchX() {
        return Gui.mc.getWindow().getScreenWidth() / 2.0F - SEARCH_WIDTH / 2.0F;
    }

    private float getSearchY() {
        return SEARCH_Y;
    }

    @Override
    public void render(GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float alpha) {
        float searchX = getSearchX();
        float searchY = getSearchY();

        RenderUtils.fillRoundRect(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 5.0F,
                Colors.bgSecondary(alpha));

        String displayText = Gui.activeSearch
                ? (Gui.searchText.isEmpty() ? "Search" : Gui.searchText)
                : "Search";

        int textColor = displayText.equals("Search")
                ? Colors.textInactive(alpha)
                : Colors.textActive(alpha);

        FontManager.get(11).drawString(guiGraphics,displayText,  searchX + 6.0F, searchY + 2.8F + 5.8F,textColor);

        renderCaret(searchX, searchY, alpha);
    }

    private void renderCaret(float searchX, float searchY, float alpha) {
        if (!Gui.activeSearch) return;
        if (System.currentTimeMillis() / 500L % 2L != 0L) return;

        float textW =   FontManager.get(11).getWidth(Gui.searchText);
        RenderUtils.fillRoundRect(searchX + 6.0F + textW, searchY + 3.0F,
                1.0F, 9.0F, 0.5F,
                Colors.accent(alpha));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        float searchX = getSearchX();
        float searchY = getSearchY();

        if (button == 0 && isHovered(mouseX, mouseY, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT)) {
            Gui.activeSearch = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!Gui.activeSearch) return false;

        if (codePoint == '\b') return true;

        if (isValidSearchChar(codePoint) && Gui.searchText.length() < 50) {
            Gui.searchText += codePoint;
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!Gui.activeSearch) return false;

        if (keyCode == 256) {
            Gui.activeSearch = false;
            Gui.searchText   = "";
            return true;
        }

        if (keyCode == 259) return true;

        return false;
    }

    @Override
    public void tick() {
        if (Gui.activeSearch) {
            processBackspaceHold();
        } else {
            resetBackspace();
        }
    }

    @Override
    public void close() {
        Gui.activeSearch = false;
        Gui.searchText   = "";
        resetBackspace();
    }

    private void processBackspaceHold() {
        boolean backspaceDown = StringUtil.isKeyDown(259);
        long now = System.currentTimeMillis();

        if (backspaceDown) {
            if (!Gui.backspaceHeld) {
                Gui.backspaceHeld           = true;
                Gui.firstBackspacePressTime = now;
                Gui.lastBackspaceTime       = now;
                deleteLastChar();
            } else if (now - Gui.firstBackspacePressTime > 500L
                    && now - Gui.lastBackspaceTime > 30L) {
                deleteLastChar();
                Gui.lastBackspaceTime = now;
            }
        } else {
            resetBackspace();
        }
    }

    private void deleteLastChar() {
        if (!Gui.searchText.isEmpty()) {
            Gui.searchText = Gui.searchText.substring(0, Gui.searchText.length() - 1);
        }
    }

    private void resetBackspace() {
        Gui.backspaceHeld           = false;
        Gui.firstBackspacePressTime = 0L;
    }

    private boolean isValidSearchChar(char c) {
        return c >= ' ' && c != 127
                && (c >= 'a' && c <= 'z'
                ||  c >= 'A' && c <= 'Z'
                ||  c >= '0' && c <= '9'
                ||  c == ' ');
    }

    private boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}