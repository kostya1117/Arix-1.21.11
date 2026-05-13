package ru.arixcompany.features.module.modules.render;

import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ColorSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.Render3dUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BreadCrumbs extends Module {

    public BreadCrumbs() {
        super("BreadCrumbs", Category.Render);
        setup(limit);
    }
    private final ValueSetting limit = new ValueSetting("Лимит (кол-во)")
            .range(10, 99999)
            .setValue(1000)
            .step(1);
    private final List<Vec3> positions = new CopyOnWriteArrayList<>();

    @EventHandler
    public void onRender3D(EventRender3D e) {
        if (positions.isEmpty()) return;

        List<Render3dUtils.LineSegment> segments = new ArrayList<>();

        for (int i = 1; i < positions.size(); i++) {
            Vec3 vec1 = positions.get(i - 1);
            Vec3 vec2 = positions.get(i);

            Color c = Arix.getInstance().getCurrentTheme().getMain();

            if (i < 10)
                c = ColorUtil.injectAlpha(c, (int) (c.getAlpha() * (i / 10f)));

            segments.add(new Render3dUtils.LineSegment(vec1, vec2, c));
        }

        Render3dUtils.renderLines(e.getMatrixStack(), segments);
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;
        if (positions.size() > limit.getValue())
            positions.remove(0);
        positions.add(new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ()));
    }

    @Override
    public void deactivate() {
        super.deactivate();
        positions.clear();
    }
}
