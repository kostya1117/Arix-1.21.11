package ru.arixcompany.utils.animation.impl.elastic;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutElastic extends Animation {
    private static final double C5 = (2 * Math.PI) / 4.5;

    public EaseInOutElastic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutElastic(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * C5)) / 2 : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * C5)) / 2 + 1;
    }
}
