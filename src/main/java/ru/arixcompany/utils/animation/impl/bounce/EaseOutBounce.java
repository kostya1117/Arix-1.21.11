package ru.arixcompany.utils.animation.impl.bounce;

import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;

public class EaseOutBounce extends Animation {
    private static final double N1 = 7.5625;
    private static final double D1 = 2.75;

    public EaseOutBounce(int duration, double endPoint) {
        super(duration, endPoint);
    }

    public EaseOutBounce(int duration, double endPoint, Direction direction) {
        super(duration, endPoint, direction);
    }

    @Override
    public double getEquation(double x1) {
        double x = x1 / this.duration;

        if (x < 1 / D1) {
            return N1 * x * x;
        } else if (x < 2 / D1) {
            return N1 * (x -= 1.5 / D1) * x + 0.75;
        } else if (x < 2.5 / D1) {
            return N1 * (x -= 2.25 / D1) * x + 0.9375;
        } else {
            return N1 * (x -= 2.625 / D1) * x + 0.984375;
        }
    }
}
