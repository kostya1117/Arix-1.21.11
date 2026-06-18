package ru.arixcompany.utils.animation.impl.expo;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutExpo extends Animation {

    public EaseOutExpo(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutExpo(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x == 1 ? 1 : 1 - Math.pow(2, -10 * x);
    }
}
