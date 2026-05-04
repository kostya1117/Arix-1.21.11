package ru.arixcompany.features.module.modules.player;

import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;

import java.awt.*;

public class TestModule extends Module {
    
    // Настройки
    private final BindSetting testbind = new BindSetting("Test bind Setting");
    private final BooleanSetting testboolean = new BooleanSetting("Боалеан");
    private final ColorSetting testColor = new ColorSetting("Цвет сеттинг", Color.red.getRGB());
    private final ListSetting testList = new ListSetting("Лист").value("One", "Two", "Three","sdsdsd","s233","sds","23232").selected("One", "Two");
    private final SelectSetting testSelect = new SelectSetting("Мод").value("1Аб", "2Аб", "3Аб","1","sdsdsds","sdsd","s233232");
    private final ValueSetting valueSetting = new ValueSetting("Слайдер").range(1, 5).setStep(1);
    private final TextSetting textSetting = new TextSetting("Текст Сеттинг").setText("1");

    public TestModule() {
        super("TestModule", Category.Player);
        setup(testbind, testboolean, testColor, testList, testSelect, valueSetting, textSetting);
    }
}