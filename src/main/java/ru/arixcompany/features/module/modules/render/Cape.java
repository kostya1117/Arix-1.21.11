package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import net.minecraft.world.entity.Entity;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.module.modules.render.cape.CapeMovement;
import ru.arixcompany.features.module.modules.render.cape.CapeStyle;
import ru.arixcompany.features.module.modules.render.cape.WindMode;
import ru.arixcompany.features.repos.FriendRepo;

public class Cape extends Module {

    public static final int CAPE_PART_COUNT = 16;
    private static Cape instance;

    private final SelectSetting movementSetting = new SelectSetting("Движение")
            .value("Обычный", "Симуляция 3D")
            .selected("Симуляция 3D");

    private final SelectSetting styleSetting = new SelectSetting("Стиль")
            .value("Блочный", "Гладкий")
            .selected("Гладкий");

    private final ListSetting targets = new ListSetting("Показывать")
            .value("Себе", "Друзьям")
            .selected("Себе", "Друзьям");

    private final SelectSetting windSetting = new SelectSetting("Ветер")
            .value("Нет", "Волны")
            .selected("Нет");

    private final ValueSetting gravitySetting = (ValueSetting) new ValueSetting("Гравитация")
            .range(1, 100).step(1);

    private final ValueSetting heightSetting = (ValueSetting) new ValueSetting("Высота")
            .range(1, 20).step(1);

    private final ValueSetting straveSetting = (ValueSetting) new ValueSetting("Боковое движение")
            .range(1, 10).step(1);

    public Cape() {
        super("Cape", Category.Render);
        instance = this;
        gravitySetting.setValue(25);
        heightSetting.setValue(6);
        straveSetting.setValue(2);
        setup(movementSetting, styleSetting, targets, windSetting, gravitySetting, heightSetting, straveSetting);
    }

    public static boolean isEnabled() {
        return instance != null && instance.isState();
    }

    public static CapeMovement getMovement() {
        if (instance == null || !instance.isState()) return CapeMovement.VANILLA;
        return switch (instance.movementSetting.getSelected()) {
            case "Обычный" -> CapeMovement.VANILLA;
            default -> CapeMovement.BASIC_SIMULATION_3D;
        };
    }

    public static CapeStyle getStyle() {
        if (instance == null || !instance.isState()) return CapeStyle.SMOOTH;
        return instance.styleSetting.isSelected("Блочный") ? CapeStyle.BLOCKY : CapeStyle.SMOOTH;
    }

    public static WindMode getWindMode() {
        if (instance == null || !instance.isState()) return WindMode.NONE;
        return instance.windSetting.isSelected("Волны") ? WindMode.WAVES : WindMode.NONE;
    }

    public static int getGravity() {
        if (instance == null || !instance.isState()) return 25;
        return instance.gravitySetting.getInt();
    }

    public static int getHeightMultiplier() {
        if (instance == null || !instance.isState()) return 6;
        return instance.heightSetting.getInt();
    }

    public static int getStraveMultiplier() {
        if (instance == null || !instance.isState()) return 2;
        return instance.straveSetting.getInt();
    }

    public static boolean shouldRender(Entity entity) {
        if (instance == null || !instance.isState()) return false;
        boolean self = entity == mc.player;
        boolean friend = FriendRepo.isFriend(entity);
        for (String s : instance.targets.getSelected()) {
            if (s.equals("Себе") && self) return true;
            if (s.equals("Друзьям") && friend) return true;
        }
        return false;
    }
}
