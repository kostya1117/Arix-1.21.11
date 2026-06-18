package ru.arixcompany.features.module.modules.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.features.event.player.EventTotemPop;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventParticleUpdate;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.utils.Textures;
import ru.arixcompany.utils.render.particle.Particle3D;
import ru.arixcompany.utils.render.particle.ParticleSystem;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Particles extends Module {
    
    public final BooleanSetting randomColor = new BooleanSetting("Случайный цвет").setValue( false);

    public final SelectSetting particleMode = new SelectSetting("Режим частиц")
            .value("Кубы", "Корона", "Взрыв кубов", "Доллары", "Сердца",
                    "Молния", "Линия", "Ромб", "Снежинки", "Звезды",
                    "Звезды альт", "Треугольник", "Случайный");

    public final SelectSetting glowMode = new SelectSetting("Режим свечения")
            .value("Оба", "Блум", "Блум образец", "Нет");

    public final BooleanSetting spinning = new BooleanSetting("Вращение").setValue(true);

    public final BooleanSetting attackTrigger = new BooleanSetting("При атаке").setValue( true);
    public final BooleanSetting totemTrigger = new BooleanSetting("При тотеме").setValue( true);
    public final BooleanSetting walkTrigger = new BooleanSetting("При ходьбе").setValue( false);
    public final BooleanSetting projectileTrigger = new BooleanSetting("При снарядах").setValue( false);

    public final ValueSetting attackAmount = new ValueSetting("Частиц при атаке")
            .range(10, 80).setValue(30).step(1)
            .visible(() -> attackTrigger.isValue());
    public final ValueSetting walkAmount = new ValueSetting("Частиц при ходьбе")
            .range(0, 60).setValue(15).step(1)
            .visible(() -> walkTrigger.isValue());
    public final ValueSetting totemAmount = new ValueSetting("Частиц при тотеме")
            .range(10, 100).setValue(40).step(5)
            .visible(() -> totemTrigger.isValue());

    public final ValueSetting spread = new ValueSetting("Разброс")
            .range(0.2f, 3.0f).setValue(1.0f).step(0.1f);
    public final ValueSetting speed = new ValueSetting("Скорость")
            .range(0.1f, 4.0f).setValue(1.0f).step(0.1f);
    public final ValueSetting lifeTime = new ValueSetting("Время жизни (события)")
            .range(0.3f, 8.0f).setValue(2.0f).step(0.2f);
    public final ValueSetting size = new ValueSetting("Размер (события)")
            .range(0.2f, 2.0f).setValue(0.6f).step(0.1f);

    public final BooleanSetting worldParticles = new BooleanSetting("Частицы в мире").setValue(true);
    public final BooleanSetting worldPhysics = new BooleanSetting("Физика мира").setValue(false);
    public final SelectSetting worldMode = new SelectSetting("Режим мировых частиц")
            .value("Кубы", "Корона", "Взрыв кубов", "Доллары", "Сердца",
                    "Молния", "Линия", "Ромб", "Снежинки", "Звезды",
                    "Звезды альт", "Треугольник", "Случайный");
    public final ValueSetting worldAmount = new ValueSetting("Кол-во мировых частиц")
            .range(10, 500).setValue(150).step(10)
            .visible(() -> worldParticles.isValue());
    public final ValueSetting worldLifeTime = new ValueSetting("Время жизни (мир)")
            .range(2.0f, 60.0f).setValue(15.0f).step(1.0f)
            .visible(() -> worldParticles.isValue());
    public final ValueSetting worldSize = new ValueSetting("Размер (мир)")
            .range(0.1f, 1.5f).setValue(0.5f).step(0.1f)
            .visible(() -> worldParticles.isValue());
    public final ValueSetting worldGlowSize = new ValueSetting("Свечение мировых")
            .range(0.1f, 8.0f).setValue(2.0f).step(0.2f)
            .visible(() -> worldParticles.isValue() && !glowMode.isSelected("Нет"));
    public final ValueSetting glowSize = new ValueSetting("Свечение размер")
            .range(0.5f, 12.0f).setValue(3.0f).step(0.5f)
            .visible(() -> !glowMode.isSelected("Нет"));

    private final GroupSetting eventGroup = new GroupSetting("При ударе частицы",spinning,
            attackTrigger, walkTrigger, projectileTrigger,
            attackAmount, walkAmount, spread, speed, lifeTime, size, glowSize);
    private final GroupSetting totemGroup = new GroupSetting("При тотеме частицы",
            totemTrigger, totemAmount);
    private final GroupSetting worldGroup = new GroupSetting("В мире частицы",
            worldParticles, worldPhysics, worldMode, worldAmount, worldLifeTime, worldSize, worldGlowSize);

    public Particles() {
        super("Particles", Category.Render);
        setup(randomColor, particleMode, glowMode,
                eventGroup, totemGroup, worldGroup);
    }

    @Override
    public void deactivate(){
        ParticleSystem.Settings s = ParticleSystem.getInstance().getSettings();
        s.setEnabled(false);
        super.deactivate();
    }
    @Override
    public void activate(){
        ParticleSystem.Settings s = ParticleSystem.getInstance().getSettings();
        s.setEnabled(true);
        super.activate();
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        ParticleSystem.getInstance().spawnAttackParticles(e.getTarget());
    }
    @EventHandler
    public void onAttack(EventTotemPop e) {
        ParticleSystem.getInstance().spawnTotemParticles(e.getEntity());
    }
}