package ru.arixcompany.ui.draggable;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.repos.alerts.AlertDraggable;
import ru.arixcompany.ui.draggable.draggables.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DraggableRepo {

    private final List<IDraggable> components = new ArrayList<>();

    @Getter
    private ArrayListDraggable arrayList;
    @Getter
    private ArmorHudDraggable armorHud;
    @Getter
    private BossBarDraggable bossbar;
    @Getter
    private ScoreboardDraggable scoreboard;
    @Getter
    private TargetHudDraggable targetHud;
    @Getter
    private AlertDraggable alert;
    @Getter
    private HotkeysDraggable hotkeys;
    @Getter
    private PotionsDraggable potions;
    @Getter
    private ChatDraggable chat;

    private IDraggable draggingComponent;

    public void init() {
        clear();

        arrayList = register(new ArrayListDraggable());
        armorHud = register(new ArmorHudDraggable());
        bossbar = register(new BossBarDraggable());
        scoreboard = register(new ScoreboardDraggable());
        targetHud = register(new TargetHudDraggable());
        alert = register(new AlertDraggable());
        hotkeys = register(new HotkeysDraggable());
        potions = register(new PotionsDraggable());
        chat = register(new ChatDraggable());
    }

    public <T extends IDraggable> T register(T c) {
        if (c != null && !components.contains(c)) components.add(c);
        return c;
    }

    public void clear() {
        releaseAll();
        components.clear();
        arrayList = null;
    }

    public List<IDraggable> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public List<DraggableComponent> getDraggableComponents() {
        List<DraggableComponent> result = new ArrayList<>();
        for (IDraggable c : components) {
            if (c instanceof DraggableComponent dc) {
                result.add(dc);
            }
        }
        return result;
    }

    public void bringToFront(IDraggable c) {
        if (c != null && components.remove(c)) components.add(c);
    }

    public void updateAll() {
        for (IDraggable c : components) {
            c.update();
        }
    }

    public void renderAll(GuiGraphics g, int mx, int my, float delta) {
        for (IDraggable c : components) {
            if (c.shouldRender()) c.renderComponent(g, mx, my, delta);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = components.size() - 1; i >= 0; i--) {
            IDraggable c = components.get(i);
            if (c.mouseClicked(mx, my, btn)) {
                if (c.isDragging()) {
                    draggingComponent = c;
                    bringToFront(c);
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        if (draggingComponent != null) {
            boolean h = draggingComponent.mouseReleased(mx, my, btn);
            draggingComponent = null;
            return h;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).mouseReleased(mx, my, btn)) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int btn) {
        return draggingComponent != null && draggingComponent.mouseDragged(mx, my, btn);
    }

    public boolean mouseScrolled(double mx, double my, double sy) {
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).mouseScrolled(mx, my, sy)) return true;
        }
        return false;
    }

    public void releaseAll() {
        for (IDraggable c : components) {
            c.stopDragging();
            if (c instanceof DraggableComponent dc) {
                dc.closeSettings();
            }
        }
        draggingComponent = null;
    }
}