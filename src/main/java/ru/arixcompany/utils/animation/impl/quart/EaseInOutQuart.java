package ru.arixcompany.utils.animation.impl.quart;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseInOutQuart extends Animation {

    public EaseInOutQuart(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseInOutQuart(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;
        return x < 0.5 ? 8.0 * Math.pow(x, 4) : 1.0 - Math.pow(-2.0 * x + 2.0, 4) / 2.0;
    }
}
