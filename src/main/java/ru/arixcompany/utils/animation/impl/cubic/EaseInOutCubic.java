package ru.arixcompany.utils.animation.impl.cubic;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutCubic extends Animation {

    public EaseInOutCubic(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutCubic(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x < 0.5 ? 4.0 * Math.pow(x, 3) : 1.0 - Math.pow(-2.0 * x + 2.0, 3) / 2.0;
    }
}
