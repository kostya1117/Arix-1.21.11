package ru.arixcompany.utils.animation.impl.quint;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutQuint extends Animation {

    public EaseInOutQuint(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutQuint(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x < 0.5 ? 16.0 * Math.pow(x, 5) : 1.0 - Math.pow(-2.0 * x + 2.0, 5) / 2.0;
    }
}
