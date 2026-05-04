package ru.arixcompany.clickgui.components.module;

import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.clickgui.components.module.settings.*;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.*;

public final class SettingComponentFactory {

    private final ColorSettingComponent.ColorPickerPositionProvider positionProvider;

    public SettingComponentFactory(ColorSettingComponent.ColorPickerPositionProvider provider) {
        this.positionProvider = provider;
    }

    public IComponent create(Setting setting) {
        if (setting instanceof BooleanSetting s) return new BooleanSettingComponent(s);
        if (setting instanceof BindSetting s) return new BindSettingComponent(s);
        if (setting instanceof ValueSetting s) return new ValueSettingComponent(s);
        if (setting instanceof SelectSetting s) return new SelectSettingComponent(s);
        if (setting instanceof TextSetting  s) return new StringSettingComponent(s);
        if (setting instanceof ListSetting s) return new ListSettingComponent(s);

        if (setting instanceof ColorSetting s) {
            ColorSettingComponent comp = new ColorSettingComponent(s);
            comp.setPositionProvider(positionProvider);
            return comp;
        }

        throw new IllegalArgumentException("Unknown setting: " + setting.getClass().getName());
    }
}