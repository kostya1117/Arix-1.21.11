package ru.arixcompany.features.module.modules.render;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.back.EaseOutBack;

public class Animations extends Module {

    public static final ListSetting animations = new ListSetting("Анимации")
            .value("Чат", "Перспектива", "Зум", "Таб", "Хотбар", "Инв");

    private static final EaseOutBack invAnim = new EaseOutBack(250, 1.0);
    private static boolean invWasOpen = false;

    public Animations() {
        super("Animations", Category.Render);
        setup(animations);
    }

    public static boolean isEnabled(String name) {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Animations mod = Arix.getInstance().getModuleRepo().getModule(Animations.class);
        return mod != null && mod.isState() && animations.isSelected(name);
    }

    public static float getInventoryAnimation() {
        if (!isEnabled("Инв")) return 1f;
        return invAnim.getOutput();
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;
        boolean isOpen = mc.screen instanceof InventoryScreen;
        if (isOpen != invWasOpen) {
            invWasOpen = isOpen;
            if (isEnabled("Инв")) {
                invAnim.setDirection(isOpen ? Direction.FORWARDS : Direction.BACKWARDS);
            }
        }
    }
}
