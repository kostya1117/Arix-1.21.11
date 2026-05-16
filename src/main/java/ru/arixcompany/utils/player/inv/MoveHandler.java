package ru.arixcompany.utils.player.inv;

import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import ru.arixcompany.utils.IMinecraft;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class MoveHandler implements IMinecraft {
    private final Set<String> lockRequests = new HashSet<>();

    public void lockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            lockRequests.add(moduleName);
            setMovementKeys(false);
        }
    }

    public void unlockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            lockRequests.remove(moduleName);
            if (lockRequests.isEmpty() && mc.screen == null) {
                setMovementKeys(true);
            }
        }
    }

    private void setMovementKeys(boolean state) {
        KeyMapping[] movementKeys = new KeyMapping[]{
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump,
                mc.options.keySprint
        };

        for (KeyMapping keyBinding : movementKeys) {
            boolean pressed = state && InputConstants.isKeyDown(mc.getWindow(), keyBinding.getDefaultKey().getValue());
            keyBinding.setDown(pressed);
        }
    }
}
