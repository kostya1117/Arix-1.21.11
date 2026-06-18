package ru.arixcompany.utils.animation.impl.back;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInBack extends Animation {
    private static final double C1 = 1.70158;
    private static final double C3 = C1 + 1.0;

    public EaseInBack(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInBack(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return C3 * Math.pow(x, 3) - C1 * Math.pow(x, 2);
    }
}
