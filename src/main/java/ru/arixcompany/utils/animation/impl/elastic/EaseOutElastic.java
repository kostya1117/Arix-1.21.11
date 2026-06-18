package ru.arixcompany.utils.animation.impl.elastic;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutElastic extends Animation {
    private static final double C4 = (2 * Math.PI) / 3;

    public EaseOutElastic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutElastic(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x == 0 ? 0 : x == 1 ? 1 : Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * C4) + 1;
    }
}
