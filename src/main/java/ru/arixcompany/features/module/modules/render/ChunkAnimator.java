//package ru.arixcompany.features.module.modules.render;
//
//import ru.arixcompany.features.module.Category;
//import ru.arixcompany.features.module.Module;
//import ru.arixcompany.features.module.setting.implement.BooleanSetting;
//import ru.arixcompany.features.module.setting.implement.SelectSetting;
//import ru.arixcompany.features.module.setting.implement.ValueSetting;
//import ru.arixcompany.utils.animation.Interpolation;
//
//public class ChunkAnimator extends Module {
//
//    private static ChunkAnimator instance;
//
//    private final SelectSetting modeSetting = new SelectSetting("Режим")
//            .value("Снизу", "Сверху", "Гибрид", "Скольжение", "Скольжение (черед.)")
//            .selected("Снизу");
//
//    private final SelectSetting easingSetting = new SelectSetting("Интерполяция")
//            .value("Линейная", "Квадратичная", "Кубическая", "Синус", "Экспо", "Назад", "Эластик", "Отскок")
//            .selected("Квадратичная");
//
//    private final ValueSetting durationSetting = (ValueSetting) new ValueSetting("Длительность")
//            .range(100, 5000).step(50);
//
//    private final BooleanSetting disableAroundPlayer = new BooleanSetting("Отключить вокруг игрока");
//
//    public ChunkAnimator() {
//        super("ChunkAnimator", Category.Render);
//        instance = this;
//        durationSetting.setValue(1000);
//        setup(modeSetting, easingSetting, durationSetting, disableAroundPlayer);
//    }
//
//    public static boolean shouldAnimate() {
//        return instance != null && instance.isState();
//    }
//
//    public static AnimationMode getMode() {
//        if (instance == null || !instance.isState()) return AnimationMode.BELOW;
//        return switch (instance.modeSetting.getSelected()) {
//            case "Сверху" -> AnimationMode.ABOVE;
//            case "Гибрид" -> AnimationMode.HYBRID;
//            case "Скольжение" -> AnimationMode.HORIZONTAL_SLIDE;
//            case "Скольжение (черед.)" -> AnimationMode.HORIZONTAL_SLIDE_ALTERNATE;
//            default -> AnimationMode.BELOW;
//        };
//    }
//
//    public static Interpolation.Curve getEasing() {
//        if (instance == null || !instance.isState()) return Interpolation.Curve.QUAD;
//        return switch (instance.easingSetting.getSelected()) {
//            case "Квадратичная" -> Interpolation.Curve.QUAD;
//            case "Кубическая" -> Interpolation.Curve.CUBIC;
//            case "Синус" -> Interpolation.Curve.SINE;
//            case "Экспо" -> Interpolation.Curve.EXPO;
//            case "Назад" -> Interpolation.Curve.BACK;
//            case "Эластик" -> Interpolation.Curve.ELASTIC;
//            case "Отскок" -> Interpolation.Curve.BOUNCE;
//            default -> Interpolation.Curve.LINEAR;
//        };
//    }
//
//    public static int getDuration() {
//        if (instance == null || !instance.isState()) return 1000;
//        return instance.durationSetting.getInt();
//    }
//
//    public static boolean isDisableAroundPlayer() {
//        return instance != null && instance.isState() && instance.disableAroundPlayer.value;
//    }
//}
