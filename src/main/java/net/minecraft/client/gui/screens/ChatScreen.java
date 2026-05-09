package net.minecraft.client.gui.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AnimatedEditBox;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.optifine.Config;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.StringUtils;
import ru.arixcompany.Arix;
import ru.arixcompany.features.draggable.DraggableRepo;

public class ChatScreen extends Screen {
    private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");

    private String historyBuffer = "";
    private int historyPos = -1;
    protected EditBox input;
    protected String initial;
    protected boolean isDraft;
    protected ExitReason exitReason = ExitReason.INTERRUPTED;
    CommandSuggestions commandSuggestions;

    public ChatScreen(String initial, boolean isDraft) {
        super(Component.translatable("chat_screen.title"));
        this.initial = initial;
        this.isDraft = isDraft;
    }

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();

        this.input = new AnimatedEditBox(
                this.minecraft.fontFilterFishy,
                4, this.height - 12,
                this.width - 4, 12,
                Component.translatable("chat.editBox")
        ) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage()
                        .append(ChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(this::onEdited);
        this.input.addFormatter(this::formatChat);
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);

        this.commandSuggestions = new CommandSuggestions(
                this.minecraft, this, this.input, this.font,
                false, false, 1, 10, true, -805306368
        );
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(int w, int h) {
        this.initial = this.input.getValue();
        this.init(w, h);
    }

    @Override
    public void onClose() {
        this.exitReason = ExitReason.INTENTIONAL;
        super.onClose();
    }

    @Override
    public void removed() {
        this.minecraft.gui.getChat().resetChatScroll();
        this.initial = this.input.getValue();

        if (this.shouldDiscardDraft() || StringUtils.isBlank(this.initial)) {
            this.minecraft.gui.getChat().discardDraft();
        } else if (!this.isDraft) {
            this.minecraft.gui.getChat().saveAsDraft(this.initial);
        }

        Arix.getInstance().getDraggableRepo().releaseAll();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(2, this.height - 14, this.width - 2, this.height - 2,
                this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));

        this.minecraft.gui.getChat().render(
                g, this.font,
                this.minecraft.gui.getGuiTicks(),
                mouseX, mouseY, true,
                this.insertionClickMode()
        );

        super.render(g, mouseX, mouseY, delta);

        this.commandSuggestions.render(g, mouseX, mouseY);
        renderDraggables(g, mouseX, mouseY, delta);
    }

    private void renderDraggables(GuiGraphics g, int mouseX, int mouseY, float delta) {
        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo == null) return;

        repo.updateAll();
        repo.renderAll(g, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics g, int x, int y, float delta) {
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();

        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo != null && repo.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }

        if (this.commandSuggestions.mouseClicked(event)) return true;

        if (button == 0) {
            ActiveTextCollector.ClickableStyleFinder finder =
                    new ActiveTextCollector.ClickableStyleFinder(
                            this.getFont(), mx, my
                    ).includeInsertions(this.insertionClickMode());

            this.minecraft.gui.getChat().captureClickableText(
                    finder,
                    this.minecraft.getWindow().getGuiScaledHeight(),
                    this.minecraft.gui.getGuiTicks(),
                    true
            );

            Style style = finder.result();
            if (style != null && this.handleComponentClicked(style, this.insertionClickMode())) {
                this.initial = this.input.getValue();
                return true;
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo != null && repo.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo != null && repo.mouseDragged(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double clamped = Mth.clamp(scrollY, -1.0, 1.0);

        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo != null && repo.mouseScrolled(mouseX, mouseY, clamped)) {
            return true;
        }

        if (this.commandSuggestions.mouseScrolled(clamped)) return true;

        double scroll = this.minecraft.hasShiftDown() ? clamped : clamped * 7.0;
        this.minecraft.gui.getChat().scrollChat((int) scroll);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.commandSuggestions.keyPressed(event)) return true;

        if (this.isDraft && event.key() == 259) {
            this.input.setValue("");
            this.isDraft = false;
            return true;
        }

        if (super.keyPressed(event)) return true;

        if (event.isConfirmation()) {
            this.handleChatInput(this.input.getValue(), true);
            this.exitReason = ExitReason.DONE;
            this.minecraft.setScreen(null);
            return true;
        }

        return switch (event.key()) {
            case 264 -> { moveInHistory(1);  yield true; }
            case 265 -> { moveInHistory(-1); yield true; }
            case 266 -> { this.minecraft.gui.getChat().scrollChat( this.minecraft.gui.getChat().getLinesPerPage() - 1); yield true; }
            case 267 -> { this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1); yield true; }
            default  -> false;
        };
    }


    private void onEdited(String text) {
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.isDraft = false;
    }

    private FormattedCharSequence formatChat(String text, int cursor) {
        return this.isDraft
                ? FormattedCharSequence.forward(text, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true))
                : null;
    }

    @Override
    public void insertText(String text, boolean replace) {
        if (replace) this.input.setValue(text);
        else         this.input.insertText(text);
    }

    public void moveInHistory(int delta) {
        int i = Mth.clamp(this.historyPos + delta, 0, this.minecraft.gui.getChat().getRecentChat().size());
        if (i == this.historyPos) return;

        if (i == this.minecraft.gui.getChat().getRecentChat().size()) {
            this.historyPos = i;
            this.input.setValue(this.historyBuffer);
        } else {
            if (this.historyPos == this.minecraft.gui.getChat().getRecentChat().size()) {
                this.historyBuffer = this.input.getValue();
            }
            this.historyPos = i;
            this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
            this.commandSuggestions.setAllowSuggestions(false);
        }
    }

    public void handleChatInput(String msg, boolean addToHistory) {
        if (this.checkCustomCommand(msg)) {
            this.minecraft.gui.getChat().addRecentChat(msg);
            return;
        }

        msg = this.normalizeChatMessage(msg);
        if (msg.isEmpty()) return;

        if (addToHistory) this.minecraft.gui.getChat().addRecentChat(msg);

        if (msg.startsWith("/")) this.minecraft.player.connection.sendCommand(msg.substring(1));
        else                     this.minecraft.player.connection.sendChat(msg);
    }

    public String normalizeChatMessage(String msg) {
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(msg.trim()));
    }

    private boolean checkCustomCommand(String msg) {
        if (msg == null) return false;
        msg = msg.trim();

        if (msg.equals("/reloadShaders")) {
            if (Config.isShaders()) { Shaders.uninit(); Shaders.loadShaderPack(); }
            return true;
        }
        if (msg.equals("/reloadChunks")) {
            this.minecraft.levelRenderer.allChanged();
            return true;
        }
        return false;
    }

    private boolean insertionClickMode() {
        return this.minecraft.hasShiftDown();
    }

    private boolean handleComponentClicked(Style style, boolean insertionMode) {
        ClickEvent clickEvent = style.getClickEvent();

        if (insertionMode) {
            if (style.getInsertion() != null) this.insertText(style.getInsertion(), false);
            return false;
        }

        if (clickEvent == null) return false;

        if (clickEvent instanceof ClickEvent.Custom custom && custom.id().equals(ChatComponent.QUEUE_EXPAND_ID)) {
            var listener = this.minecraft.getChatListener();
            if (listener.queueSize() != 0L) listener.acceptNextDelayedMessage();
        } else {
            defaultHandleGameClickEvent(clickEvent, this.minecraft, this);
        }

        return true;
    }

    protected boolean shouldDiscardDraft() {
        return this.exitReason != ExitReason.INTERRUPTED
                && (this.exitReason != ExitReason.INTENTIONAL || !this.minecraft.options.saveChatDrafts().get());
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getTitle());
        output.add(NarratedElementType.USAGE, USAGE_TEXT);
        String s = this.input.getValue();
        if (!s.isEmpty()) {
            output.nest().add(NarratedElementType.TITLE,
                    Component.translatable("chat_screen.message", s));
        }
    }

    @Override public boolean isPauseScreen()     { return false; }
    @Override public boolean isAllowedInPortal() { return true;  }

    @FunctionalInterface
    public interface ChatConstructor<T extends ChatScreen> {
        T create(String initial, boolean isDraft);
    }

    protected enum ExitReason { INTENTIONAL, INTERRUPTED, DONE }
}