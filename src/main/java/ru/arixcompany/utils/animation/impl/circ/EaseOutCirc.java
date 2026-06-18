package ru.arixcompany.utils.animation.impl.circ;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutCirc extends Animation {

    public EaseOutCirc(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutCirc(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return Math.sqrt(1 - Math.pow(x - 1, 2));
    }
}
