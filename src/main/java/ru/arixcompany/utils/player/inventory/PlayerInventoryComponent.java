package ru.arixcompany.utils.player.inventory;

import lombok.experimental.UtilityClass;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.TimerUtils;
import ru.arixcompany.utils.player.MoveUtils;

import java.util.ArrayDeque;
import java.util.Deque;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {

    private static final long DEFAULT_PRE_DELAY = 100L;
    private static final long DEFAULT_POST_DELAY = 100L;

    private final TimerUtils timer = new TimerUtils();
    private final Deque<InventoryTask> taskQueue = new ArrayDeque<>();

    private InventoryTask currentTask;
    private boolean active;
    private boolean executed;
    private boolean sprintBlocked;

    public void onUpdate() {
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        if (!active && currentTask == null) {
            currentTask = taskQueue.pollFirst();
        }

        if (currentTask == null) {
            sprintBlocked = false;
            return;
        }

        if (!active) {
            if (!shouldDelay()) {
                runCurrentTask();
                finishCurrentTask();
                return;
            }

            active = true;
            executed = false;
            sprintBlocked = true;
            suppressSprint();
            timer.reset();
            return;
        }

        suppressSprint();

        long elapsed = timer.getElapsed();

        if (!executed && elapsed >= currentTask.preDelay) {
            executed = true;
            runCurrentTask();
            timer.reset();
        }

        if (executed && timer.getElapsed() >= currentTask.postDelay) {
            finishCurrentTask();
        }
    }

    public void addTask(Runnable task) {
        addTask(task, DEFAULT_PRE_DELAY, DEFAULT_POST_DELAY);
    }

    public void addTask(Runnable task, long preDelay, long postDelay) {
        if (task == null) return;

        taskQueue.addLast(new InventoryTask(
                task,
                Math.max(preDelay, 0L),
                Math.max(postDelay, 0L)
        ));
    }

    public boolean isBusy() {
        return active || currentTask != null || !taskQueue.isEmpty();
    }

    public boolean isSprintBlocked() {
        return sprintBlocked;
    }

    public void clear() {
        taskQueue.clear();
        currentTask = null;
        active = false;
        executed = false;
        sprintBlocked = false;
        restoreSprint();
    }

    private boolean shouldDelay() {
        return MoveUtils.isMoving();
    }

    private void runCurrentTask() {
        if (currentTask == null || currentTask.action == null) return;

        try {
            currentTask.action.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishCurrentTask() {
        restoreSprint();
        sprintBlocked = false;
        active = false;
        executed = false;
        currentTask = null;
    }

    private void suppressSprint() {
        if (mc.player == null) return;

        mc.player.setSprinting(false);

        AutoSprint autoSprint = Arix.getInstance().getModuleRepo().getModule(AutoSprint.class);
        if (autoSprint != null && autoSprint.isState()) {
            autoSprint.sprint = false;
        }
    }

    private void restoreSprint() {
        AutoSprint autoSprint = Arix.getInstance().getModuleRepo().getModule(AutoSprint.class);
        if (autoSprint != null && autoSprint.isState() && !autoSprint.sprint) {
            autoSprint.sprint = true;
        }
    }

    private static class InventoryTask {
        private final Runnable action;
        private final long preDelay;
        private final long postDelay;

        private InventoryTask(Runnable action, long preDelay, long postDelay) {
            this.action = action;
            this.preDelay = preDelay;
            this.postDelay = postDelay;
        }
    }
}