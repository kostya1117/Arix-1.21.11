package ru.arixcompany.utils.render;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Stack;

public class ScissorUtil {
    private static final Stack<ScissorState> scissorStack = new Stack<>();

    private static class ScissorState {
        GuiGraphics graphics;
        int x1, y1, x2, y2;

        ScissorState(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
            this.graphics = graphics;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static void start(GuiGraphics graphics, double x, double y, double width, double height) {
        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = (int) (x + width);
        int y2 = (int) (y + height);

        if (!scissorStack.isEmpty()) {
            ScissorState parent = scissorStack.peek();
            x1 = Math.max(x1, parent.x1);
            y1 = Math.max(y1, parent.y1);
            x2 = Math.min(x2, parent.x2);
            y2 = Math.min(y2, parent.y2);

            if (x1 >= x2 || y1 >= y2) {
                x1 = x2 = y1 = y2 = 0;
            }
        }

        scissorStack.push(new ScissorState(graphics, x1, y1, x2, y2));
        graphics.enableScissor(x1, y1, x2, y2);
    }

    public static void end() {
        if (!scissorStack.isEmpty()) {
            ScissorState current = scissorStack.pop();
            current.graphics.disableScissor();

            if (!scissorStack.isEmpty()) {
                ScissorState parent = scissorStack.peek();
                parent.graphics.enableScissor(parent.x1, parent.y1, parent.x2, parent.y2);
            }
        }
    }

    public static void clear() {
        while (!scissorStack.isEmpty()) {
            ScissorState state = scissorStack.pop();
            state.graphics.disableScissor();
        }
    }
}
