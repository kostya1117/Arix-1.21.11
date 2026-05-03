package ru.arixcompany.module.modules.player;

import ru.arixcompany.event.EventHandler;
import ru.arixcompany.event.player.EventKey;
import ru.arixcompany.module.Category;
import ru.arixcompany.module.Module;
import ru.arixcompany.module.setting.implement.*;

import java.awt.*;

public class TestModule extends Module {
    public TestModule() {
        super("TestModule", Category.Player);
        setup(testbind,testboolean,testColor,testList,testSelect,sliderSetting,textSetting);
    }

    BindSetting testbind = new BindSetting("Test");
    BooleanSetting testboolean = new BooleanSetting("Болнка");
    ColorSetting testColor = new ColorSetting("цвет", Color.red.getRGB());
    MultiSelectSetting testList = new MultiSelectSetting("Лист").selected("S","1","3");
    SelectSetting testSelect = new SelectSetting("Мод").value("1","2","3");
    SliderSetting sliderSetting = new SliderSetting("СЛайдер").range(1,5).setStep(1);
    TextSetting textSetting = new TextSetting("Текст").setText("1");
}
