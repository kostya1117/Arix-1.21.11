package ru.arixcompany.utils.animation.impl.sine;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutSine extends Animation {

    public EaseInOutSine(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutSine(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return -(Math.cos(Math.PI * x) - 1) / 2;
    }
}
