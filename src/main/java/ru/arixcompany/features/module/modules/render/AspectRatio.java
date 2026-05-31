package ru.arixcompany.features.module.modules.render;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

public class AspectRatio extends Module {
    public static final SelectSetting aspect = new SelectSetting(
            "Соотношение экрана")
            .value("16:9", "4:3", "1:1", "16:10", "21:9", "32:9", "5:4", "2:1", "Кастомное");
    public static final ValueSetting customAspect = new
            ValueSetting("Кастомое значние")
            .range( 1.0F, 6.0F)
            .setStep(1)
            .visible(() -> aspect.isSelected("Кастомное"));

    public AspectRatio() {
        super("AspectRatio", Category.Render);
        setup(aspect, customAspect);
    }

    public static float getAspectRation() {
        if (!Arix.getInstance().getModuleRepo().getModule(AspectRatio.class).isState()) {
            return 0.0F;
        } else {
            float aspect1 = (float) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getGuiScaledHeight();
            String var3 = aspect.getSelected();

            float newAspect = switch (var3) {
                case "16:9" -> 1.7777778F;
                case "4:3" -> 1.3333334F;
                case "1:1" -> 1.0F;
                case "16:10" -> 1.6F;
                case "21:9" -> 2.3333333F;
                case "32:9" -> 3.5555556F;
                case "5:4" -> 1.25F;
                case "2:1" -> 2.0F;
                default -> customAspect.getValue();
            };
            return newAspect - aspect1;
        }
    }
}