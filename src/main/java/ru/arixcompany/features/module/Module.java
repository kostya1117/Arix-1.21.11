package ru.arixcompany.features.module;

import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.module.setting.SettingAdder;
import ru.arixcompany.features.repos.sound.SoundRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

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

    public final Animation animation = new EaseInOutQuad(200, 1.0);
    public final Animation mAnim     = new EaseInOutQuad(240, 1.0);

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.state = false;
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

//        if (mc.player != null) {
//            Main.getInstance().getNotificationRepo().success(this.name + " включен");
//            SoundUtil.playSound_wav("on", 0.35F);
//        }
    }

    public void deactivate() {
        EventRepo.unregister(this);

        SoundRepo.playOff();

//        if (mc.player != null) {
//            Main.getInstance().getNotificationRepo().error(this.name + " выключен");
//        }
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
}