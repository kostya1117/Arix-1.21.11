package ru.arixcompany.features.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.module.setting.SettingAdder;
import ru.arixcompany.features.repos.SoundRepo;
import ru.arixcompany.features.repos.alerts.AlertRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.impl.quad.EaseInOutQuad;

@Getter
@Setter
public class Module extends SettingAdder implements IMinecraft {
    @Getter
    public String name;
    public int bind = -1;
    @Getter
    public boolean state;
    public Category category;
    public boolean binding;

    public enum BindMode { TOGGLE, HOLD }
    @Getter
    @Setter
    public BindMode bindMode = BindMode.TOGGLE;

    public final Animation animation = new EaseInOutQuad(200, 1.0);
    public final Animation mAnim     = new EaseInOutQuad(240, 1.0);

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.state = false;
    }

    public void onBindPress() {
        if (bindMode == BindMode.HOLD) {
            if (!state) setState(true);
        } else {
            toggle();
        }
    }

    public void onBindRelease() {
        if (bindMode == BindMode.HOLD && state) {
            setState(false);
        }
    }

    public void activate() {
        try {
            EventRepo.register(this);
        } catch (Exception e) {
            e.printStackTrace();
            state = false;
            return;
        }

        SoundRepo.playOn();
        AlertRepo.activate(name + ChatFormatting.GREEN + " включен");
    }

    public void deactivate() {
        EventRepo.unregister(this);

        SoundRepo.playOff();
        AlertRepo.deactivate(name + ChatFormatting.RED + " выключен");
    }

    public void toggle() {
        this.state = !this.state;
        if (this.state) {
            this.activate();
        } else {
            this.deactivate();
        }
    }

    public void setState(boolean enable) {
        this.state = enable;
        if (enable) {
            this.activate();
        } else {
            this.deactivate();
        }
    }

    public boolean isForced() {
        return false;
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.level == null;
    }
}