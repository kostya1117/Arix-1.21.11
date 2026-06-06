package ru.arixcompany.utils.math;

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventPriority;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RequestHandler;
import ru.arixcompany.utils.IMinecraft;

public class Timer implements IMinecraft {
    public static final Timer INSTANCE = new Timer();

    private final RequestHandler<Float> requestHandler = new RequestHandler<>();

    public Timer() {
        EventRepo.register(this);
    }

    /**
     * You cannot set this manually. Use [requestTimerSpeed] instead.
     */
    public float getTimerSpeed() {
        Float value = requestHandler.getActiveRequestValue();
        return value != null ? value : 1.0f;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(EventGameTicked event) {
        requestHandler.tick();
    }

    /**
     * Requests a timer speed change. If another module requests with a higher priority,
     * the other module is prioritized.
     *
     * @param timerSpeed      Target timer speed (e.g., 1.5f for 150% speed).
     * @param priority        Higher = higher priority.
     * @param provider        The module/requester (must implement RequestHandler.RequestProvider).
     * @param resetAfterTicks After how many ticks the request expires.
     */
    public void requestTimerSpeed(float timerSpeed, int priority, RequestHandler.RequestProvider provider, int resetAfterTicks) {
        requestHandler.request(
                new RequestHandler.Request<>(
                        resetAfterTicks + 1, // this prevents requests from being instantly removed
                        priority,
                        provider,
                        timerSpeed
                )
        );
    }

    /**
     * Overload for default resetAfterTicks = 1
     */
    public void requestTimerSpeed(float timerSpeed, int priority, RequestHandler.RequestProvider provider) {
        requestTimerSpeed(timerSpeed, priority, provider, 1);
    }
}