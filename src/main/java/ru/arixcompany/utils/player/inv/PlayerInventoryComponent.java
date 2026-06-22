package ru.arixcompany.utils.player.inv;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.repos.ScriptRepo;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.player.MoveUtils;
import ru.arixcompany.utils.player.NetworkUtil;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {
    public final List<KeyMapping> moveKeys = List.of(mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump);

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

    public void addTaskMs(Runnable task, long preDelayMs, long postDelayMs) {
        if (MoveUtils.isMoving()) {
            ScriptRepo.ScriptTask script = new ScriptRepo.ScriptTask();
            Arix.getInstance().getScriptRepo().addTask(script);

            script.schedule(EventUpdate.class, eventUpdate -> {
                MoveUtils.lockMovement("inv");
                return true;
            });

            if (preDelayMs > 0) {
                script.scheduleDelayed(EventUpdate.class, eventUpdate -> {
                    return true;
                }, preDelayMs);
            }

            script.schedule(EventUpdate.class, eventUpdate -> {
                task.run();
                return true;
            });

            if (postDelayMs > 0) {
                script.scheduleDelayed(EventUpdate.class, eventUpdate -> {
                    return true;
                }, postDelayMs);
            }

            script.schedule(EventUpdate.class, eventUpdate -> {
                MoveUtils.unlockMovement("inv");
                return true;
            });

            return;
        }

        task.run();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyMapping -> keyMapping.setDown(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyMapping -> keyMapping.setDown(InputConstants.isKeyDown(mc.getWindow(), keyMapping.getDefaultKey().getValue())));
    }
}
