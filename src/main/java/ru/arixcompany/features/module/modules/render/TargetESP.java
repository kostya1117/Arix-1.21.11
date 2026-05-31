package ru.arixcompany.features.module.modules.render;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.render.targetEsp.TargetEspMode;
import ru.arixcompany.features.module.modules.render.targetEsp.impl.CircleMode;
import ru.arixcompany.features.module.modules.render.targetEsp.impl.CrystalsMode;
import ru.arixcompany.features.module.modules.render.targetEsp.impl.SpiritsMode;
import ru.arixcompany.features.module.modules.render.targetEsp.impl.SquareMode;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class TargetESP extends Module {
    private final SelectSetting mode = new SelectSetting("Режим")
            .value("Квадрат", "Призраки", "Кристаллы", "Круг");

    private final Map<String, TargetEspMode> modes = Map.of(
            "Квадрат", new SquareMode(),
            "Призраки", new SpiritsMode(),
            "Кристаллы", new CrystalsMode(),
            "Круг", new CircleMode()
    );

    public TargetESP() {
        super("TargetESP", Category.Render);

        List<Setting> allSettings = new ArrayList<>(List.of(mode));
        modes.values().forEach(targetMode -> {
            List<Setting> modeSettings = targetMode.getSettings();
            modeSettings.forEach(setting ->
                    setting.setVisible(() -> mode.isSelected(targetMode.getName()))
            );
            allSettings.addAll(modeSettings);
        });

        setup(allSettings.toArray(new Setting[0]));
    }

    @EventHandler
    public void onRender(EventRender3D e) {
        if (mc.level == null || mc.player == null) return;

        HitAura hitAura = Arix.getInstance().getModuleRepo().getModule(HitAura.class);
        if (hitAura == null) return;

        LivingEntity target = HitAura.getTarget();
        TargetEspMode currentMode = modes.get(mode.getSelected());

        if (currentMode != null) {
            currentMode.render(e, target, e.getTickDelta());
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        TargetEspMode currentMode = modes.get(mode.getSelected());
        if (currentMode != null) {
            currentMode.onUpdate();
        }
    }
}
