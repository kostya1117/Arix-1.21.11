package ru.arixcompany.features.module.modules.combat.aura.aiming;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Mirrors LiquidBounce's RequestHandler<T>.
 */
public class RequestHandler<T> {

    private int currentTick = 0;
    private final PriorityBlockingQueue<Request<T>> activeRequests;

    public RequestHandler() {
        this.activeRequests = new PriorityBlockingQueue<>(11,
                Comparator.comparingInt((Request<T> req) -> -req.getPriority())
        );
    }

    public void tick(int deltaTime) {
        currentTick += deltaTime;
    }

    public void tick() {
        tick(1);
    }

    public void request(Request<T> request) {
        // we remove all requests provided by module on new request
        activeRequests.removeIf(req -> req.getProvider() == request.getProvider());
        request.setExpiresIn(request.getExpiresIn() + currentTick);
        activeRequests.add(request);
    }

    public T getActiveRequestValue() {
        Request<T> top = activeRequests.peek();
        if (top == null) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            while (top.getExpiresIn() <= currentTick || !top.getProvider().isRunning()) {
                activeRequests.remove();
                top = activeRequests.peek();
                if (top == null) {
                    return null;
                }
            }
        }

        return top.getValue();
    }

    @Getter
    @Setter
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