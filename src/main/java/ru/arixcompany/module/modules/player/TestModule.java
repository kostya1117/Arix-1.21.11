package ru.arixcompany.module.modules.player;

import ru.arixcompany.event.EventHandler;
import ru.arixcompany.event.player.EventKey;
import ru.arixcompany.module.Category;
import ru.arixcompany.module.Module;
import ru.arixcompany.module.setting.implement.*;

import java.awt.*;

public class TestModule extends Module {
    
    // Настройки
    private final BindSetting testbind = new BindSetting("Test bind Setting");
    private final BooleanSetting testboolean = new BooleanSetting("Боалеан");
    private final ColorSetting testColor = new ColorSetting("Цвет сеттинг", Color.red.getRGB());
    private final MultiSelectSetting testList = new MultiSelectSetting("Лист").value("One", "Two", "Three").selected("One", "Two");
    private final SelectSetting testSelect = new SelectSetting("Мод").value("1Аб", "2Аб", "3Аб");
    private final SliderSetting sliderSetting = new SliderSetting("Слайдер").range(1, 5).setStep(1);
    private final TextSetting textSetting = new TextSetting("Текст Сеттинг").setText("1");

    public TestModule() {
        super("TestModule", Category.Player);
        setup(testbind, testboolean, testColor, testList, testSelect, sliderSetting, textSetting);
    }
}