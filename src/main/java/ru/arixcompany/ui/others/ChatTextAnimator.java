package ru.arixcompany.ui.others;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.util.ArrayList;
import java.util.List;

public class ChatTextAnimator {

    private static final float Y_OFFSET = 4f;
    private static final int   ANIM_MS  = 150;

    private final List<CharState> chars = new ArrayList<>();
    private String lastText = "";

    public void sync(String text) {
        if (text.equals(lastText)) return;

        int newLen = text.length();
        int alive = 0;
        for (CharState c : chars) {
            if (!c.removing) alive++;
        }

        if (newLen > alive) {
            for (int i = alive; i < newLen; i++) {
                chars.add(new CharState(text.charAt(i)));
            }
        }

        if (newLen < alive) {
            int toRemove = alive - newLen;
            for (int i = chars.size() - 1; i >= 0 && toRemove > 0; i--) {
                CharState c = chars.get(i);
                if (!c.removing) {
                    c.removing = true;
                    c.alpha.setDirection(Direction.BACKWARDS);
                    c.offset.setDirection(Direction.BACKWARDS);
                    toRemove--;
                }
            }
        }

        int charIdx = 0;
        for (CharState c : chars) {
            if (!c.removing && charIdx < newLen) {
                c.ch = text.charAt(charIdx);
                charIdx++;
            }
        }

        lastText = text;
    }

    public void update() {
        chars.removeIf(c -> c.removing && c.alpha.finished(Direction.BACKWARDS));
    }

    public void render(GuiGraphics g, int x, int y, int color, boolean showCursor) {
        Font font = Minecraft.getInstance().font;
        int cx = x;

        for (CharState c : chars) {
            float a = (float) c.alpha.getOutput();
            if (a <= 0.005f) continue;

            float oProgress = (float) c.offset.getOutput();
            float yOff;
            if (c.removing) {
                yOff = -Y_OFFSET * (1f - oProgress);
            } else {
                yOff = Y_OFFSET * (1f - oProgress);
            }

            int baseAlpha = ARGB.alpha(color);
            int finalAlpha = (int) (baseAlpha * a);
            int col = ARGB.color(finalAlpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color));

            String ch = String.valueOf(c.ch);
            g.drawString(font, ch, cx, y + (int) yOff, col, false);
            cx += font.width(ch);
        }

        if (showCursor && (System.currentTimeMillis() / 300) % 2 == 0) {
            g.drawString(font, "_", cx, y, color, false);
        }
    }

    public void clear() {
        chars.clear();
        lastText = "";
    }

    private static class CharState {
        char ch;
        boolean removing;
        final Animation alpha;
        final Animation offset;

        CharState(char ch) {
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