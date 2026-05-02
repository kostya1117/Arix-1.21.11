package ru.arixcompany.module;

import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.event.EventRepo;
import ru.arixcompany.module.setting.SettingAdder;
import ru.arixcompany.utils.IMinecraft;

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

//        if (mc.player != null) {
//            Main.getInstance().getNotificationRepo().success(this.name + " включен");
//            SoundUtil.playSound_wav("on", 0.35F);
//        }
    }

    public void deactivate() {
        EventRepo.unregister(this);

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