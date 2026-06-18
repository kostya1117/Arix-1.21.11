package ru.arixcompany.utils.animation.impl.quint;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInQuint extends Animation {

    public EaseInQuint(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInQuint(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return Math.pow(x, 5);
    }
}
