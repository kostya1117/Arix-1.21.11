package ru.arixcompany.utils.animation.impl.quart;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInQuart extends Animation {

    public EaseInQuart(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInQuart(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return Math.pow(x, 4);
    }
}
