package ru.arixcompany;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

public class TestScreen extends Screen {

    public TestScreen() {
        super(Component.literal("Test Screen"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = 20;

        // ========================================================
        // 1. Основная карточка
        // ========================================================
        int cardX = centerX - 160;
        int cardY = startY;
        int cardW = 320;
        int cardH = 260;

        CustomFont title = FontManager.get(FontManager.Fonts.SF, 24);
        CustomFont regular = FontManager.get(FontManager.Fonts.SF);
        CustomFont small = FontManager.get(FontManager.Fonts.SF, 12);

        // Тень
        RenderUtils.drawShadow(cardX, cardY, cardW, cardH, 12, 8, 0xFF000000);

        // Фон карточки
        RenderUtils.fillRoundRect(cardX, cardY, cardW, cardH, 12, 0xE6101218);

        // Акцентная полоска сверху
        RenderUtils.fillRoundRectGradient(cardX, cardY, cardW, 4, 12,
                0xFF5865F2, 0xFF7C3AED
        );

        // ========================================================
        // 2. Заголовок
        // ========================================================
        int contentX = cardX + 20;
        int contentY = cardY + 20;

        if (title != null) {
            title.drawString(g, "Arix Client", contentX, contentY, 0xFFFFFFFF, true);
        }
        contentY += 35;

        // Разделитель
        RenderUtils.fillRoundRect(contentX, contentY, cardW - 40, 1, 0, 0x33FFFFFF);
        contentY += 12;

        // ========================================================
        // 3. Текст разного размера
        // ========================================================
        if (regular != null) {
            regular.drawString(g, "Regular font - 16px", contentX, contentY, 0xFFE0E0E0);
            contentY += 22;
        }

        if (small != null) {
            small.drawString(g, "Small font - 12px - abcdefghijklmnopqrstuvwxyz", contentX, contentY, 0xFFE0E0E0);
            contentY += 22;
        }

        if (small != null) {
            small.drawString(g, "Small font - 12px - абвгдежзийклмнопрстуфхцчшщъыьэюя", contentX, contentY, 0xFF999999);
            contentY += 18;
        }

        contentY += 5;

        // ========================================================
        // 4. Кнопки
        // ========================================================
        int btnW = 120;
        int btnH = 30;
        int btnGap = 10;
        int btnX1 = cardX + 20;
        int btnX2 = btnX1 + btnW + btnGap;

        // Кнопка 1 — Primary
        boolean hovered1 = isHovered(mouseX, mouseY, btnX1, contentY, btnW, btnH);
        RenderUtils.fillRoundRect(btnX1, contentY, btnW, btnH, 8,
                hovered1 ? 0xFF4752C4 : 0xFF5865F2
        );
        if (regular != null) {
            regular.drawCenteredString(g, "абвгдежзийклмнопрстуфхцчшщъыьэюя", btnX1 + btnW / 2.0F, contentY + 8, 0xFFFFFFFF);
        }

        // Кнопка 2 — Outlined
        boolean hovered2 = isHovered(mouseX, mouseY, btnX2, contentY, btnW, btnH);
        if (hovered2) {
            RenderUtils.fillRoundRect(btnX2, contentY, btnW, btnH, 8, 0x205865F2);
        }
        RenderUtils.drawRoundRectOutline(btnX2, contentY, btnW, btnH, 8, 1, 0xFF5865F2);
        if (regular != null) {
            regular.drawCenteredString(g, "Outline", btnX2 + btnW / 2.0F, contentY + 8, 0xFF5865F2);
        }

        contentY += btnH + 15;

        // ========================================================
        // 5. Прогресс-бар
        // ========================================================
        int barW = cardW - 40;
        int barH = 8;
        float progress = (float)(Math.sin(System.currentTimeMillis() / 1000.0) + 1.0) / 2.0F;

        RenderUtils.fillRoundRect(contentX, contentY, barW, barH, 4, 0x33FFFFFF);
        RenderUtils.fillRoundRectGradient(contentX, contentY, (int)(barW * progress), barH, 4,
                0xFF5865F2, 0xFF7C3AED
        );

        if (regular != null) {
            regular.drawString(g, (int)(progress * 100) + "%", contentX + barW + 8, contentY - 1, 0xFFAAAAAA);
        }

        contentY += barH + 15;

        // ========================================================
        // 6. Информация
        // ========================================================
        if (regular != null) {
            regular.drawString(g, "Mouse: " + mouseX + ", " + mouseY, contentX, contentY, 0xFF666666);
            contentY += 14;
            regular.drawString(g, "FPS: " + this.minecraft.getFps(), contentX, contentY, 0xFF666666);
            contentY += 14;
            regular.drawString(g, "Width: " + regular.getWidth("Hello, World!") + "px", contentX, contentY, 0xFF666666);
        }

        // ========================================================
        // 7. Маленькие карточки внизу
        // ========================================================
        int miniY = cardY + cardH + 15;
        int miniW = 100;
        int miniH = 60;

        for (int i = 0; i < 3; i++) {
            int mx = centerX - 160 + i * (miniW + 10);
            boolean miniHover = isHovered(mouseX, mouseY, mx, miniY, miniW, miniH);

            RenderUtils.drawShadow(mx, miniY, miniW, miniH, 8, 4, 0xFF000000);
            RenderUtils.fillRoundRect(mx, miniY, miniW, miniH, 8,
                    miniHover ? 0xE61A1A2E : 0xCC1A1A2E
            );

            if (small != null) {
                small.drawCenteredString(g, "Card " + (i + 1), mx + miniW / 2.0F, miniY + 12, 0xFFE0E0E0);
            }

            if (small != null) {
                small.drawCenteredString(g, "Info here", mx + miniW / 2.0F, miniY + 30, 0xFF888888);
            }
        }

        // ========================================================
        // 8. Тултип при наведении на кнопку
        // ========================================================
        if (hovered1 && small != null) {
            renderTooltip(g, mouseX, mouseY, "Click me!", small);
        }
    }

    private void renderTooltip(GuiGraphics g, int mx, int my, String text, CustomFont font) {
        int tw = (int)(font.getWidth(text) + 12);
        int th = (int)(font.getHeight() + 8);
        int tx = mx + 10;
        int ty = my - th - 4;

        RenderUtils.drawShadow(tx, ty, tw, th, 4, 3, 0xFF000000);
        RenderUtils.fillRoundRect(tx, ty, tw, th, 4, 0xF0101218);
        RenderUtils.drawRoundRectOutline(tx, ty, tw, th, 4, 1, 0x33FFFFFF);

        font.drawString(g, text, tx + 6, ty + 4, 0xFFE0E0E0);
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}