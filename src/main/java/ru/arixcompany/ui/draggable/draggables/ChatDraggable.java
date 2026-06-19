package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.List;

public class ChatDraggable extends DraggableComponent {

    private static final float FONT_SIZE = 10f;
    private static final int BOTTOM_MARGIN = 40;
    private static final int MAX_CHAT_LINES = 10000;

    private final SelectSetting bgMode = new SelectSetting("Фон")
            .value("Без", "Полный", "Контур")
            .selected("Полный");

    private final List<ChatLineEntry> chatLines = new ArrayList<>();
    private long maxSeenTime = 0;

    private static class ChatLineEntry {
        final GuiMessage.Line line;
        int count;

        ChatLineEntry(GuiMessage.Line line) {
            this.line = line;
            this.count = 1;
        }
    }

    public ChatDraggable() {
        super("Чат", 0, 0, 320, 180);
        setPinned(true);
        setup(bgMode);
    }

    @Override
    protected void updateVisibility() {
        this.visible = isCustomChatActive();
    }

    public static boolean isCustomChatActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Чат");
    }

    @Override
    public void update() {
        super.update();
        syncChatLines();
    }

    private void syncChatLines() {
        if (mc.gui == null || mc.gui.getChat() == null) return;
        var trimmed = mc.gui.getChat().trimmedMessages;
        if (trimmed.isEmpty()) return;

        long localMax = maxSeenTime;

        List<GuiMessage.Line> newLines = new ArrayList<>();
        for (GuiMessage.Line line : trimmed) {
            if (line.addedTime() > localMax) {
                localMax = line.addedTime();
            }
            if (line.addedTime() > maxSeenTime) {
                newLines.add(line);
            }
        }

        if (newLines.isEmpty()) return;

        for (int i = newLines.size() - 1; i >= 0; i--) {
            addWithDuplicateCheck(newLines.get(i));
        }

        maxSeenTime = localMax;

        while (chatLines.size() > MAX_CHAT_LINES) {
            chatLines.remove(chatLines.size() - 1);
        }
    }

    private void addWithDuplicateCheck(GuiMessage.Line line) {
        if (!chatLines.isEmpty()) {
            String newText = toPlainString(line.content());
            String lastText = toPlainString(chatLines.get(0).line.content());
            if (newText.equals(lastText)) {
                chatLines.get(0).count++;
                return;
            }
        }
        chatLines.add(0, new ChatLineEntry(line));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!canInteract()) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mc.getWindow() == null) return false;
        int sh = mc.getWindow().getGuiScaledHeight();
        int chatWidth = ChatComponent.getWidth(mc.options.chatWidth().get());
        int chatHeight = ChatComponent.getHeight(
            mc.screen instanceof ChatScreen ? mc.options.chatHeightFocused().get() : mc.options.chatHeightUnfocused().get()
        );
        float scale = mc.options.chatScale().get().floatValue();
        float scaledW = chatWidth / scale;
        float scaledH = chatHeight / scale;
        float bottomY = sh - BOTTOM_MARGIN;
        float topY = bottomY - scaledH;
        return mouseX >= 0 && mouseX <= scaledW + 4 && mouseY >= topY && mouseY <= bottomY;
    }

    @Override
    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mc.gui == null || mc.gui.getChat() == null) return false;
        double clamped = Mth.clamp(scrollY, -1.0, 1.0);
        double scroll = mc.hasShiftDown() ? clamped : clamped * 7.0;
        mc.gui.getChat().scrollChat((int) scroll);
        return true;
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.gui == null || mc.gui.getChat() == null) return;

        var chatLinesLocal = chatLines;
        if (chatLinesLocal.isEmpty()) return;

        CustomFont font = FontManager.get(FONT_SIZE);

        float chatScale = mc.options.chatScale().get().floatValue();
        float chatOpacity = mc.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
        float textBgOpacity = mc.options.textBackgroundOpacity().get().floatValue();
        double lineSpacing = mc.options.chatLineSpacing().get();
        int lineHeight = (int) (FONT_SIZE * (lineSpacing + 1.0));
        int chatWidth = ChatComponent.getWidth(mc.options.chatWidth().get());
        int screenHeight = graphics.guiHeight();

        float scaledChatWidth = chatWidth / chatScale;
        float scaledBottom = (screenHeight - BOTTOM_MARGIN) / chatScale;

        String mode = bgMode.getSelected();
        int scrollPos = mc.gui.getChat().chatScrollbarPos;
        int visibleLines = mc.gui.getChat().getLinesPerPage();

        graphics.pose().pushMatrix();
        graphics.pose().scale(chatScale, chatScale);
        graphics.pose().translate(0.0F, 0.0F);

        int startIdx = Math.min(chatLinesLocal.size() - scrollPos, visibleLines) - 1;

        for (int k = startIdx; k >= 0; k--) {
            int idx = k + scrollPos;
            if (idx >= chatLinesLocal.size()) continue;

            ChatLineEntry entry = chatLinesLocal.get(idx);
            GuiMessage.Line line = entry.line;

            float lineAlpha = 1f;

            float lineY = scaledBottom - (k + 1) * lineHeight;

            Component textComponent = toComponent(line.content());
            if (entry.count > 1) {
                textComponent = Component.literal("")
                    .append(textComponent)
                    .append(Component.literal(" x" + entry.count).withStyle(ChatFormatting.GRAY));
            }
            float textWidth = font.getComponentWidth(textComponent);

            float bgAlpha = alpha * textBgOpacity * lineAlpha;

            if (!mode.equals("Без") && bgAlpha > 0.005f) {
                float bgX;
                float bgW;
                if (mode.equals("Контур")) {
                    bgX = 0;
                    bgW = textWidth + 6;
                } else {
                    bgX = -2;
                    bgW = scaledChatWidth + 4;
                }
                int bgColor = ARGB.black(bgAlpha);
                Interface.drawClientRect(bgX, lineY, bgW, lineHeight, 4f, bgColor);
            }

            float textAlpha = alpha * lineAlpha * chatOpacity;
            if (textAlpha > 0.005f) {
                int textColor = ARGB.color(textAlpha, 0xFFFFFF);
                font.drawComponent(graphics, textComponent, 2, lineY, textColor);
            }
        }

        graphics.pose().popMatrix();
    }

    private static String toPlainString(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    public static Component toComponent(FormattedCharSequence seq) {
        MutableComponent root = Component.literal("");
        seq.accept((index, style, codePoint) -> {
            String text = new String(Character.toChars(codePoint));
            root.append(Component.literal(text).withStyle(style));
            return true;
        });
        return root;
    }
}
