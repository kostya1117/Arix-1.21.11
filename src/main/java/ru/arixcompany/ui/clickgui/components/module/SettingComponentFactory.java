package ru.arixcompany.ui.clickgui.components.module;

import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.ui.clickgui.components.module.settings.*;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.*;

public final class SettingComponentFactory {
    public IComponent create(Setting setting) {
        if (setting instanceof GroupSetting s) return new GroupSettingComponent(s, this);
        if (setting instanceof BooleanSetting s) return new BooleanSettingComponent(s);
        if (setting instanceof BindSetting s) return new BindSettingComponent(s);
        if (setting instanceof ValueSetting s) return new ValueSettingComponent(s);
        if (setting instanceof SelectSetting s) return new SelectSettingComponent(s);
        if (setting instanceof TextSetting  s) return new TextSettingComponent(s);
        if (setting instanceof ListSetting s) return new ListSettingComponent(s);
        if (setting instanceof ButtonSetting s) return new ButtonSettingComponent(s);

        throw new IllegalArgumentException("Неизвестная настройка : " + setting.getClass().getName());
    }
}