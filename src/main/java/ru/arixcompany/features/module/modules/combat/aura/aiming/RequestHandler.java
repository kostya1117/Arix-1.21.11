package ru.arixcompany.features.module.modules.combat.aura.aiming;

import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Mirrors LiquidBounce's RequestHandler<T>.
 */
public class RequestHandler<T> {

    private int currentTick = 0;

    // PriorityBlockingQueue для потокобезопасности (как в LiquidBounce)
    private final PriorityBlockingQueue<Request<T>> activeRequests =
            new PriorityBlockingQueue<>(11, Comparator.comparingInt(r -> -r.priority));

    public void tick() {
        tick(1);
    }

    public void tick(int deltaTime) {
        currentTick += deltaTime;
    }

    public void request(Request<T> request) {
        // Удаляем старые запросы от того же провайдера
        activeRequests.removeIf(r -> r.provider == request.provider);
        request.expiresIn += currentTick;
        activeRequests.add(request);
    }

    public T getActiveRequestValue() {
        Request<T> top = activeRequests.peek();
        if (top == null) return null;

        if (Minecraft.getInstance().isSameThread()){
            while (top != null && (top.expiresIn <= currentTick || !top.provider.isRunning())) {
                activeRequests.poll();
                top = activeRequests.peek();
            }
        }

        return top != null ? top.value : null;
    }

    /**
     * A requested state of the system.
     *
     * Note: A request is deleted when its corresponding module is disabled.
     *
     * @param expiresIn in how many ticks should this request expire?
     * @param priority  higher = higher priority
     * @param provider  module which requested value (must implement RequestProvider)
     * @param value     the requested value
     */
    public static class Request<T> {
        public int expiresIn;
        public final int priority;
        public final RequestProvider provider;
        public final T value;

        public Request(int expiresIn, int priority, RequestProvider provider, T value) {
            this.expiresIn = expiresIn;
            this.priority = priority;
            this.provider = provider;
            this.value = value;
        }
    }

    /**
     * Interface for request providers (e.g., Modules).
     * Allows the handler to automatically clean up requests when the provider is disabled.
     */
    public interface RequestProvider {
        boolean isRunning();
    }
}