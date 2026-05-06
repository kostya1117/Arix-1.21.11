package ru.arixcompany.features.module.modules.misc;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

public class ClientSounds extends Module {
    public ValueSetting volumeSetting = new ValueSetting("Громкость")
            .range(0.0f, 1.0f)
            .setValue(1.0f);
    public ValueSetting pitchSetting = new ValueSetting("Тон")
            .range(0.5f, 2.0f)
            .setValue(1.0f);

    public ClientSounds() {
        super("ClientSounds", Category.Misc);
        setup(volumeSetting, pitchSetting);
    }

    public float getVolume() {
        return volumeSetting.getValue();
    }

    public float getPitch() {
        return pitchSetting.getValue();
    }
}