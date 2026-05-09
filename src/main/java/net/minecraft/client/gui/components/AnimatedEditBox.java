package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.util.ArrayList;
import java.util.List;

public class AnimatedEditBox extends EditBox {
    private static final int ANIM_MS = 150;
    private static final float Y_OFFSET = 4f;

    private final Font font;
    private final List<CharAnim> charAnims = new ArrayList<>();
    private String lastRenderedValue = "";

    private boolean isEditable = true;
    private int textColor = 0xFFE0E0E0;
    private int textColorUneditable = 0xFF6E6E6E;
    private boolean textShadow = true;
    private Component hint;
    private String suggestion;
    private int highlightPos;

    public AnimatedEditBox(Font font, int x, int y, int w, int h, Component msg) {
        super(font, x, y, w, h, msg);
        this.font = font;
    }

    private void syncAnims() {
        String current = this.getValue();
        if (current.equals(lastRenderedValue)) {
            charAnims.removeIf(a -> a.removing && a.alpha.finished(Direction.BACKWARDS));
            return;
        }

        int newLen = current.length();
        int alive = 0;
        for (CharAnim a : charAnims) {
            if (!a.removing) alive++;
        }

        if (newLen > alive) {
            for (int i = alive; i < newLen; i++) {
                charAnims.add(new CharAnim(current.charAt(i)));
            }
        }

        if (newLen < alive) {
            int toRemove = alive - newLen;
            for (int i = charAnims.size() - 1; i >= 0 && toRemove > 0; i--) {
                CharAnim a = charAnims.get(i);
                if (!a.removing) {
                    a.removing = true;
                    a.alpha.setDirection(Direction.BACKWARDS);
                    a.offset.setDirection(Direction.BACKWARDS);
                    toRemove--;
                }
            }
        }

        int idx = 0;
        for (CharAnim a : charAnims) {
            if (!a.removing && idx < newLen) {
                a.ch = current.charAt(idx++);
            }
        }

        charAnims.removeIf(a -> a.removing && a.alpha.finished(Direction.BACKWARDS));
        lastRenderedValue = current;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        syncAnims();

        if (this.isBordered()) {
            WidgetSprites sprites = getSpritesReflect();
            if (sprites != null) {
                g.blitSprite(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        sprites.get(this.isActive(), this.isFocused()),
                        this.getX(), this.getY(), this.getWidth(), this.getHeight()
                );
            }
        }

        int baseTextColor = ensureFullAlpha(isEditable ? this.textColor : this.textColorUneditable);

        int innerW = this.getInnerWidth();
        int baseX  = this.getX() + (this.isBordered() ? 4 : 0);
        int baseY  = this.isBordered() ? this.getY() + (this.height - 8) / 2 : this.getY();

        String fullValue = this.getValue();
        int    dispPos   = getDisplayPosReflect();
        String visible   = this.font.plainSubstrByWidth(fullValue.substring(dispPos), innerW);
        int    visLen    = visible.length();

        int     cursorIdx    = this.getCursorPosition() - dispPos;
        boolean cursorInView = cursorIdx >= 0 && cursorIdx <= visLen;
        boolean cursorBlink  = this.isFocused()
                && (Util.getMillis() - getFocusedTimeReflect()) / 300L % 2L == 0L
                && cursorInView;

        int cx      = baseX;
        int skipped = 0;
        int rendered = 0;
        int cursorX = baseX;

        for (CharAnim anim : charAnims) {
            float animAlpha = (float) anim.alpha.getOutput(); // 0.0 .. 1.0

            if (anim.removing) {
                if (animAlpha > 0.005f) {
                    float oP   = (float) anim.offset.getOutput();
                    float yOff = -Y_OFFSET * (1f - oP);
                    renderChar(g, anim.ch, cx, baseY, yOff, animAlpha, baseTextColor, dispPos + skipped);
                }
                continue;
            }

            if (skipped < dispPos) { skipped++; continue; }
            if (rendered >= visLen) break;

            if (rendered == cursorIdx) cursorX = cx;

            if (animAlpha > 0.005f) {
                float oP   = (float) anim.offset.getOutput();
                float yOff = Y_OFFSET * (1f - oP);
                renderChar(g, anim.ch, cx, baseY, yOff, animAlpha, baseTextColor, dispPos + rendered);
            }

            cx += this.font.width(String.valueOf(anim.ch));
            rendered++;
        }

        if (rendered == cursorIdx || cursorIdx >= visLen) cursorX = cx;

        if (fullValue.isEmpty() && !this.isFocused() && this.hint != null) {
            g.drawString(this.font, this.hint, baseX, baseY, baseTextColor, textShadow);
        }

        if (this.suggestion != null
                && fullValue.length() < getMaxLengthReflect()
                && this.getCursorPosition() >= fullValue.length()) {
            g.drawString(this.font, this.suggestion, cursorX - 1, baseY, 0xFF7F7F7F, textShadow);
        }

        int highlightIdx = Mth.clamp(this.highlightPos - dispPos, 0, visLen);
        if (highlightIdx != cursorIdx && cursorInView) {
            int highlightX = baseX + this.font.width(visible.substring(0, highlightIdx));
            g.textHighlight(
                    Math.min(cursorX,    this.getX() + this.width),
                    baseY - 1,
                    Math.min(highlightX - 1, this.getX() + this.width),
                    baseY + 1 + 9,
                    true
            );
        }

        if (cursorBlink) {
            boolean atEnd = this.getCursorPosition() >= fullValue.length()
                    && fullValue.length() < getMaxLengthReflect();
            if (atEnd) {
                g.drawString(this.font, "_", cursorX, baseY, baseTextColor, textShadow);
            } else {
                g.fill(cursorX, baseY - 1, cursorX + 1, baseY + 1 + 9, baseTextColor);
            }
        }
    }

    private void renderChar(GuiGraphics g, char ch, int x, int y,
                            float yOffset, float alpha,
                            int baseColor, int cursorPosForFormat) {
        String str = String.valueOf(ch);
        FormattedCharSequence seq = applyFormattersReflect(str, cursorPosForFormat);

        int color = applyAlpha(baseColor, alpha);

        g.pose().pushMatrix();
        g.pose().translate(0f, yOffset);
        g.drawString(this.font, seq, x, y, color, textShadow);
        g.pose().popMatrix();
    }

    private static int ensureFullAlpha(int color) {
        return color | 0xFF000000;
    }

    private static int applyAlpha(int color, float alpha) {
        int origA = (color >> 24) & 0xFF;
        if (origA == 0) origA = 0xFF;
        int newA = (int) (origA * alpha);
        return (color & 0x00FFFFFF) | ((newA & 0xFF) << 24);
    }

    private int getDisplayPosReflect() {
        try {
            var f = EditBox.class.getDeclaredField("displayPos");
            f.setAccessible(true);
            return f.getInt(this);
        } catch (Exception e) { return 0; }
    }

    private long getFocusedTimeReflect() {
        try {
            var f = EditBox.class.getDeclaredField("focusedTime");
            f.setAccessible(true);
            return f.getLong(this);
        } catch (Exception e) { return Util.getMillis(); }
    }

    private int getMaxLengthReflect() {
        try {
            var f = EditBox.class.getDeclaredField("maxLength");
            f.setAccessible(true);
            return f.getInt(this);
        } catch (Exception e) { return 256; }
    }

    private WidgetSprites getSpritesReflect() {
        try {
            var f = EditBox.class.getDeclaredField("SPRITES");
            f.setAccessible(true);
            return (WidgetSprites) f.get(null);
        } catch (Exception e) { return null; }
    }

    private FormattedCharSequence applyFormattersReflect(String text, int pos) {
        try {
            var m = EditBox.class.getDeclaredMethod("applyFormat", String.class, int.class);
            m.setAccessible(true);
            return (FormattedCharSequence) m.invoke(this, text, pos);
        } catch (Exception e) {
            return FormattedCharSequence.forward(text, Style.EMPTY);
        }
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        this.isEditable = editable;
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        this.textColor = color;
    }

    @Override
    public void setTextColorUneditable(int color) {
        super.setTextColorUneditable(color);
        this.textColorUneditable = color;
    }

    @Override
    public void setTextShadow(boolean shadow) {
        super.setTextShadow(shadow);
        this.textShadow = shadow;
    }

    @Override
    public void setHint(Component hint) {
        super.setHint(hint);
        this.hint = hint;
    }

    @Override
    public void setSuggestion(String suggestion) {
        super.setSuggestion(suggestion);
        this.suggestion = suggestion;
    }

    @Override
    public void setHighlightPos(int pos) {
        super.setHighlightPos(pos);
        this.highlightPos = Mth.clamp(pos, 0, this.getValue().length());
    }

    private static class CharAnim {
        char ch;
        boolean removing;
        final Animation alpha;
        final Animation offset;

        CharAnim(char ch) {
            this.ch = ch;
            this.removing = false;
            this.alpha = new EaseInOutQuad(ANIM_MS, 1.0);
            this.alpha.setDirection(Direction.FORWARDS);
            this.alpha.reset();
            this.offset = new EaseInOutQuad(ANIM_MS, 1.0);
            this.offset.setDirection(Direction.FORWARDS);
            this.offset.reset();
        }
    }
}