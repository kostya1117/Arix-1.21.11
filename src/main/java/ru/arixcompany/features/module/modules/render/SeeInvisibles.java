package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

public class SeeInvisibles extends Module {
    public SeeInvisibles() {
        super("SeeInvisibles", Category.Render);
        setup(alpha);
    }

    public ValueSetting alpha = new ValueSetting("Прозрачность")
            .setValue(0.5F)
            .range(0.3f, 1)
            .setStep(0.1F);

    public float getAlpha() {
        return alpha.getValue();
    }
}