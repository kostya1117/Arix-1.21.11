package ru.arixcompany.utils.render.particle;

import java.util.ArrayDeque;
import java.util.Deque;

public class ParticlePool {

    private final Deque<Particle3D> pool = new ArrayDeque<>();
    private final int maxSize;

    public ParticlePool(int maxSize) {
        this.maxSize = maxSize;
        for (int i = 0; i < maxSize; i++) {
            pool.add(new Particle3D());
        }
    }

    public Particle3D acquire() {
        if (pool.isEmpty()) {
            return new Particle3D();
        }
        return pool.pollFirst();
    }

    public void release(Particle3D particle) {
        if (particle != null && pool.size() < maxSize) {
            particle.reset();
            pool.addLast(particle);
        }
    }

    public int size() {
        return pool.size();
    }
}