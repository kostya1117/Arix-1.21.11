package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.utils.render.RenderUtils;

public class Interface extends Module {

    public static final ListSetting elements = new ListSetting("Элементы")
            .value("Аррай лист",
                    "Броня",
                    "Боссбар",
                    "Кейбинды",
                    "Зелья",
                    "Скорбоард"
                    ,"ТаргетХуд",
                    "Уведомления");

    public BooleanSetting customButtons = new BooleanSetting("Кастомные кнопки");
    public BooleanSetting buttonSounds = new BooleanSetting("Звуки кнопок")
            .setValue(true)
            .visible(customButtons::isValue);

    public Interface() {
        super("Interface", Category.Render);
        setup(elements, customButtons, buttonSounds);
    }

    public static void drawClientRect(float x,float y,float w,float h,float r,int color) {
        RenderUtils.fillRoundRect(x, y, w, h, r, color);
    }
}