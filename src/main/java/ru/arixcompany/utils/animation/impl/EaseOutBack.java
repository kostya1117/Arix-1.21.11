package ru.arixcompany.utils.animation.impl;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutBack extends Animation {
    private static final double C1 = 1.70158;
    private static final double C3 = C1 + 1.0;

    public EaseOutBack(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutBack(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    protected double getEquation(double x1) {
        double x = x1 / this.duration;
        return 1.0 + C3 * Math.pow(x - 1.0, 3) + C1 * Math.pow(x - 1.0, 2);
    }
}