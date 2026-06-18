package ru.arixcompany.utils.animation.impl.quint;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutQuint extends Animation {

    public EaseOutQuint(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutQuint(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return 1.0 - Math.pow(1.0 - x, 5);
    }
}
