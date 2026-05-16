/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura.aiming;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Mirrors LiquidBounce's RequestHandler<T>.
 */
public class RequestHandler<T> {

    private int currentTick = 0;

    private final PriorityQueue<Request<T>> activeRequests =
        new PriorityQueue<>(11, Comparator.comparingInt(r -> -r.priority));

    public void tick() {
        tick(1);
    }

    public void tick(int deltaTime) {
        currentTick += deltaTime;
    }

    public void request(Request<T> request) {
        // we remove all requests provided by the same provider on new request
        activeRequests.removeIf(r -> r.provider == request.provider);
        request.expiresIn += currentTick;
        activeRequests.add(request);
    }

    public T getActiveRequestValue() {
        Request<T> top = activeRequests.peek();
        if (top == null) return null;

        // remove all outdated requests
        while (top != null && top.expiresIn <= currentTick) {
            activeRequests.poll();
            top = activeRequests.peek();
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
     * @param provider  object which requested the value (used for deduplication)
     * @param value     the requested value
     */
    public static class Request<T> {
        public int expiresIn;
        public final int priority;
        public final Object provider;
        public final T value;

        public Request(int expiresIn, int priority, Object provider, T value) {
            this.expiresIn = expiresIn;
            this.priority  = priority;
            this.provider  = provider;
            this.value     = value;
        }
    }
}
