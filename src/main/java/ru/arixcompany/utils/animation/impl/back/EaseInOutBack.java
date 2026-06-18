package ru.arixcompany.utils.animation.impl.back;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutBack extends Animation {
    private static final double C1 = 1.70158;
    private static final double C2 = C1 * 1.525;

    public EaseInOutBack(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutBack(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x < 0.5 ? (Math.pow(2 * x, 2) * ((C2 + 1) * 2 * x - C2)) / 2 : (Math.pow(2 * x - 2, 2) * ((C2 + 1) * (x * 2 - 2) + C2) + 2) / 2;
    }
}
