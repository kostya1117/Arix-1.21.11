package ru.arixcompany.ui.clickgui.widgets;

import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.ui.clickgui.Colors;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@NoArgsConstructor
public final class SearchComponent implements IComponent {

    private static final float SEARCH_WIDTH  = 220.0F;
    private static final float SEARCH_HEIGHT = 22.0F;
    private static final float SEARCH_Y      = 15.0F; // Отступ от верха экрана

    private float getSearchX() {
        // Используем getGuiScaledWidth() чтобы работало на любых мониторах и GUI Scale!
        return (mc.getWindow().getGuiScaledWidth() / 2.0F) - (SEARCH_WIDTH / 2.0F);
    }

    private float getSearchY() {
        return SEARCH_Y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float alpha) {
        float searchX = getSearchX();
        float searchY = getSearchY();

        // Рисуем фон
        RenderUtils.fillRoundRect(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6.0F, Colors.bgSecondary(alpha));

        // Если поиск активен, делаем красивую обводку акцентным цветом
        int outlineColor = Gui.activeSearch ? Colors.accent(alpha) : Colors.outline(alpha);
        RenderUtils.drawRoundRectOutline(searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT, 6.0F, 1.0F, outlineColor);

        // Текст поиска
        String displayText = Gui.searchText.isEmpty() ? (Gui.activeSearch ? "" : "Search modules...") : Gui.searchText;
        int textColor = Gui.searchText.isEmpty() && !Gui.activeSearch ? Colors.textInactive(alpha) : Colors.textActive(alpha);

        float textX = searchX + 10.0F;
        float textY = searchY + (SEARCH_HEIGHT / 2.0F) - (FontManager.get(12).getHeight() / 2.0F) + 1.0F;
        FontManager.get(12).drawString(guiGraphics, displayText, textX, textY, textColor);

        // Красивый мигающий курсор
        renderCaret(textX, searchY, alpha);
    }

    private void renderCaret(float textStartX, float searchY, float alpha) {
        if (!Gui.activeSearch) return;
        // Мигание каждые 500мс
        if (System.currentTimeMillis() % 1000L < 500L) {
            float textW = Gui.searchText.isEmpty() ? 0 : FontManager.get(12).getWidth(Gui.searchText);
            // Палочка курсора
            RenderUtils.fillRoundRect(textStartX + textW + 1.0F, searchY + 5.0F, 1.0F, SEARCH_HEIGHT - 10.0F, 0.5F, Colors.textActive(alpha));
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        float searchX = getSearchX();
        float searchY = getSearchY();

        // Кликнули по поиску - активируем
        if (button == 0 && isHovered(mouseX, mouseY, searchX, searchY, SEARCH_WIDTH, SEARCH_HEIGHT)) {
            Gui.activeSearch = true;
            return true;
        }

        // Кликнули в другое место - закрываем ввод поиска
        if (Gui.activeSearch && button == 0) {
            Gui.activeSearch = false;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!Gui.activeSearch) return false;

        if (codePoint == '\b') return true;

        // Лимит 40 символов и только адекватные символы
        if (isValidSearchChar(codePoint) && Gui.searchText.length() < 40) {
            Gui.searchText += codePoint;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!Gui.activeSearch) return false;

        if (keyCode == 256) { // ESC
            Gui.activeSearch = false;
            Gui.searchText   = "";
            return true;
        }

        if (keyCode == 259) return true; // Backspace обрабатывается в tick()

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
        // 259 = код Backspace в GLFW
        boolean backspaceDown = StringUtil.isKeyDown(259);
        long now = System.currentTimeMillis();

        if (backspaceDown) {
            if (!Gui.backspaceHeld) {
                Gui.backspaceHeld = true;
                Gui.firstBackspacePressTime = now;
                Gui.lastBackspaceTime = now;
                deleteLastChar();
            } else if (now - Gui.firstBackspacePressTime > 500L && now - Gui.lastBackspaceTime > 30L) {
                // Если зажали дольше 500мс, стираем быстро каждые 30мс
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
        Gui.backspaceHeld = false;
        Gui.firstBackspacePressTime = 0L;
    }

    private boolean isValidSearchChar(char c) {
        return c >= ' ' && c != 127
                && (c >= 'a' && c <= 'z'
                ||  c >= 'A' && c <= 'Z'
                ||  c >= '0' && c <= '9'
                ||  c == ' '
                ||  c == '-' || c == '_');
    }

    private boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}