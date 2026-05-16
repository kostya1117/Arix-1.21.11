package ru.arixcompany.ui.title.alt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.file.files.AltFile;
import ru.arixcompany.features.repos.AltRepo;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class AltManagerScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 4;

    private final Screen parent;
    private final AltFile altFile = new AltFile();

    private EditBox nameField;
    private EditBox searchField;

    private int selectedIndex = -1;
    private long lastClickTime = 0;
    private long loginFlashTime = 0;

    private double scrollOffset = 0;
    private static double savedScroll = 0;

    private int listX, listY, listWidth, listHeight;

    private int scrollRectX, scrollRectY, scrollRectWidth, scrollRectHeight;
    private boolean draggingScrollbar = false;

    public AltManagerScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        scrollOffset = savedScroll;

        int panelX = width / 2 - PANEL_WIDTH / 2;

        listX = panelX;
        listY = 85;
        listWidth = PANEL_WIDTH - SCROLLBAR_WIDTH - 4;
        listHeight = height - 210;

        scrollRectWidth = SCROLLBAR_WIDTH;
        scrollRectX = listX + listWidth + 2;
        scrollRectY = listY;
        scrollRectHeight = listHeight;

        try {
            altFile.loadFromFile(new File("config/arix"));
        } catch (Exception ignored) {}

        searchField = new EditBox(font, panelX, 40, PANEL_WIDTH, 20, Component.literal("Search"));
        addRenderableWidget(searchField);

        nameField = new EditBox(font, panelX, height - 110, PANEL_WIDTH, 20, Component.literal("Username"));
        nameField.setMaxLength(16);
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(Component.literal("Добавить"), b -> addAlt())
                .bounds(panelX, height - 85, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Войти"), b -> loginSelected())
                .bounds(panelX + 75, height - 85, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Рандомный"), b -> {
            String name = NickGenerator.generateRandomName();
            AltRepo.add(new AltRepo.Alt(name));
            login(name);
            save();
        }).bounds(panelX + 150, height - 85, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Очистить все"), b -> {
            AltRepo.clear();
            selectedIndex = -1;
            save();
        }).bounds(panelX, height - 60, PANEL_WIDTH, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Назад"),
                        b -> minecraft.setScreen(parent))
                .bounds(panelX, height - 35, PANEL_WIDTH, 20).build());

        autoScrollToCurrent();
    }

    private void addAlt() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;

        if (AltRepo.getAlts().stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(name))) return;

        AltRepo.add(new AltRepo.Alt(name));
        nameField.setValue("");
        save();
    }

    private void loginSelected() {
        List<AltRepo.Alt> list = getFiltered();
        if (selectedIndex >= 0 && selectedIndex < list.size()) {
            login(list.get(selectedIndex).getName());
            loginFlashTime = System.currentTimeMillis();
        }
    }

    private void login(String name) {
        SessionUtil.setSession(name);
        AltRepo.setLastAlt(name);
        save();
    }

    private void autoScrollToCurrent() {
        String current = Minecraft.getInstance().getUser().getName();
        List<AltRepo.Alt> list = AltRepo.getAlts();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase(current)) {
                scrollOffset = i * ROW_HEIGHT;
                break;
            }
        }
    }

    private List<AltRepo.Alt> getFiltered() {
        String filter = searchField.getValue().toLowerCase();
        return AltRepo.getAlts().stream()
                .filter(a -> a.getName().toLowerCase().contains(filter))
                .collect(Collectors.toList());
    }

    private void save() {
        try {
            altFile.saveToFile(new File("config/arix"));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean p_431348_) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() == 0) {
            if (mouseX >= scrollRectX && mouseX <= scrollRectX + scrollRectWidth &&
                    mouseY >= scrollRectY && mouseY <= scrollRectY + scrollRectHeight) {
                draggingScrollbar = true;
                return true;
            }
        }

        if (event.button() == 0 || event.button() == 1) {
            if (mouseX >= listX && mouseX <= listX + listWidth &&
                    mouseY >= listY && mouseY <= listY + listHeight) {

                int index = (int) ((mouseY - listY + scrollOffset) / ROW_HEIGHT);
                List<AltRepo.Alt> list = getFiltered();

                if (index >= 0 && index < list.size()) {
                    AltRepo.Alt alt = list.get(index);

                    if (event.button() == 1) {
                        AltRepo.remove(alt);
                        selectedIndex = -1;
                        save();
                        return true;
                    }

                    long time = System.currentTimeMillis();

                    if (index == selectedIndex && time - lastClickTime < 300) {
                        login(alt.getName());
                        loginFlashTime = time;
                    }

                    selectedIndex = index;
                    lastClickTime = time;
                    return true;
                }
            }
        }

        return super.mouseClicked(event, p_431348_);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent p_429390_) {
        draggingScrollbar = false;
        return super.mouseReleased(p_429390_);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double p_94699_, double p_94700_) {
        if (draggingScrollbar) {
            int contentHeight = getFiltered().size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - listHeight);
            if (maxScroll <= 0) return true;

            double percent = (event.y() - scrollRectY) / scrollRectHeight;
            percent = Math.max(0, Math.min(1, percent));

            scrollOffset = percent * maxScroll;
            return true;
        }

        return super.mouseDragged(event, p_94699_, p_94700_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= listX && mouseX <= listX + listWidth &&
                mouseY >= listY && mouseY <= listY + listHeight) {

            int maxScroll = Math.max(0, getFiltered().size() * ROW_HEIGHT - listHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - deltaY * 15));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void removed() {
        savedScroll = scrollOffset;
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        String current = Minecraft.getInstance().getUser().getName();

        g.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF);
        g.drawCenteredString(font, "Текущий: " + current, width / 2, 28, 0xFFAAAAAA);

        int hintX = listX - 130;
        int hintY = listY + 4;
        g.drawString(font, "Управление:", hintX, hintY, 0xFFFFFFFF);
        g.drawString(font, "• ЛКМ: Выбрать", hintX, hintY + 14, 0xFFAAAAAA);
        g.drawString(font, "• 2хЛКМ: Войти", hintX, hintY + 26, 0xFFAAAAAA);
        g.drawString(font, "• ПКМ: Удалить", hintX, hintY + 38, 0xFFFF5555); // Выделим красным для наглядности
        g.drawString(font, "• Колесо: Скролл", hintX, hintY + 50, 0xFFAAAAAA);

        List<AltRepo.Alt> list = getFiltered();

        int contentHeight = list.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        g.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        for (int i = 0; i < list.size(); i++) {

            int y = listY + i * ROW_HEIGHT - (int) scrollOffset;
            if (y + ROW_HEIGHT < listY || y > listY + listHeight) continue;

            String name = list.get(i).getName();
            boolean selected = i == selectedIndex;
            boolean currentAlt = name.equalsIgnoreCase(current);

            int bg = 0x33000000;
            if (selected) bg = 0x5500FF00;
            if (currentAlt) bg = 0x3300AAFF;
            if (System.currentTimeMillis() - loginFlashTime < 500 && currentAlt)
                bg = 0x8800FF00;

            g.fill(listX, y, listX + listWidth, y + ROW_HEIGHT, bg);
            g.drawString(font, name, listX + 6, y + 6,
                    currentAlt ? 0xFF00FF00 : 0xFFFFFFFF);
        }

        g.disableScissor();

        if (maxScroll > 0) {
            g.fill(scrollRectX, scrollRectY,
                    scrollRectX + scrollRectWidth,
                    scrollRectY + scrollRectHeight,
                    0x22000000);

            int thumbHeight = Math.max(20,
                    (int)((float)listHeight / contentHeight * scrollRectHeight));

            int thumbY = scrollRectY +
                    (int)((scrollOffset / maxScroll) *
                            (scrollRectHeight - thumbHeight));

            g.fill(scrollRectX,
                    thumbY,
                    scrollRectX + scrollRectWidth,
                    thumbY + thumbHeight,
                    draggingScrollbar ? 0xFF00AAFF : 0xFF888888);
        }

        super.render(g, mouseX, mouseY, partial);
    }
}