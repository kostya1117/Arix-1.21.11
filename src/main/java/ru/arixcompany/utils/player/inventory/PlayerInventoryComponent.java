package ru.arixcompany.utils.player.inventory;

import lombok.experimental.UtilityClass;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.repos.ScriptRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.player.MoveUtils;
import ru.arixcompany.utils.player.NetworkUtil;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {

    public void addTask(Runnable task) {
        ScriptRepo.ScriptTask script = new ScriptRepo.ScriptTask();
        if (MoveUtils.isMoving()) {
            Arix.getInstance().getScriptRepo().addTask(script);
            if (NetworkUtil.isFunTime() || NetworkUtil.isCopyTime()) {
                script.schedule(EventUpdate.class, eventUpdate -> {
                    MoveUtils.lockMovement("inv");
                    return true;
                });
                script.schedule(EventUpdate.class, eventUpdate -> {
                    task.run();
                    MoveUtils.unlockMovement("inv");
                    return true;
                });
                return;
            }
            if (NetworkUtil.isReallyWorld()) {
                if (mc.player.onGround()) {

                    script.schedule(EventUpdate.class, eventUpdate -> {
                        MoveUtils.lockMovement("inv");
                        return true;
                    });

                    script.schedule(EventUpdate.class, eventUpdate -> {
                        MoveUtils.unlockMovement("inv");
                        return true;
                    });

                    script.schedule(EventUpdate.class, eventUpdate -> {
                        task.run();
                        return true;
                    });
                    script.schedule(EventUpdate.class, eventUpdate -> {
                        MoveUtils.lockMovement("inv");
                        return true;
                    });
                    return;
                }
            }
        }
        task.run();
    }

    public void addTask(Runnable task, int preDelayTicks, int postDelayTicks) {
        if (MoveUtils.isMoving()) {
            ScriptRepo.ScriptTask script = new ScriptRepo.ScriptTask();
            Arix.getInstance().getScriptRepo().addTask(script);

            script.schedule(EventUpdate.class, eventUpdate -> {
                MoveUtils.lockMovement("inv");
                return true;
            });

            for (int i = 0; i < preDelayTicks; i++) {
                script.schedule(EventUpdate.class, eventUpdate -> {
                    return true;
                });
            }

            script.schedule(EventUpdate.class, eventUpdate -> {
                task.run();
                return true;
            });

            for (int i = 0; i < postDelayTicks; i++) {
                script.schedule(EventUpdate.class, eventUpdate -> {
                    return true;
                });
            }

            script.schedule(EventUpdate.class, eventUpdate -> {
                MoveUtils.unlockMovement("inv");
                return true;
            });

            return;
        }

        task.run();
    }
}