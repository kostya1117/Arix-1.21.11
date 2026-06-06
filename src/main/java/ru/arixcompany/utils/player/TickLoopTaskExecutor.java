package ru.arixcompany.utils.player;

import lombok.Getter;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Schedules tasks that must run inside [net.minecraft.client.Minecraft.tick].
 */
public final class TickLoopTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger("Arix/TickLoopTaskExecutor");

    @Getter
    private static volatile boolean isInTickLoop = false;
    private static final Deque<Runnable> pendingTasks = new ArrayDeque<>();

    public static void executeInTickLoop(Runnable runnable) {
        if (isInTickLoop) {
            runnable.run();
            return;
        }

        Minecraft.getInstance().execute(() -> {
            synchronized (pendingTasks) {
                pendingTasks.addLast(runnable);
            }
        });
    }

    public static void onTickLoopStart() {
        isInTickLoop = true;

        while (true) {
            Runnable task;
            synchronized (pendingTasks) {
                task = pendingTasks.pollFirst();
            }

            if (task == null) break;

            try {
                task.run();
            } catch (ReportedException e) {
                throw e;
            } catch (Throwable t) {
                LOGGER.error("Unhandled exception thrown by tick-loop task", t);
            }
        }
    }

    public static void onTickLoopCompleted() {
        isInTickLoop = false;
    }
}