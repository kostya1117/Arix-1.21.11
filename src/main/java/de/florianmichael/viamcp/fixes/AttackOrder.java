package de.florianmichael.viamcp.fixes;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
// import de.florianmichael.viamcp.ViaMCP; // Если ViaMCP не используется напрямую здесь, импорт не нужен
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.InteractionHand;

public class AttackOrder {
    // private static final Minecraft mc = Minecraft.getInstance(); // Можно так, если используется часто
    // Либо получать экземпляр по месту, что более безопасно в статических методах, если класс загружается очень рано

    /**
     * Sends a swing packet if the hit result is not an entity.
     * @param hitResult The result of the ray trace.
     * @param hand The hand to swing.
     */
    public static void sendConditionalSwing(HitResult hitResult, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { // Всегда хорошо проверять на null
            return;
        }

        if (hitResult != null && hitResult.getType() != HitResult.Type.ENTITY) {
            mc.player.swing(hand);
        }
    }

    /**
     * Sends an attack packet with a swing, order depends on the protocol version.
     * @param attacker The player performing the attack (usually mc.player).
     * @param target The entity being attacked.
     * @param hand The hand to swing.
     */
    public static void sendFixedAttack(Player attacker, Entity target, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || attacker == null || target == null) { // Проверки
            return;
        }

        // Убедимся, что ViaLoadingBase и его targetVersion инициализированы
        ViaLoadingBase viaInstance = ViaLoadingBase.getInstance();
        if (viaInstance == null || viaInstance.getTargetVersion() == null) {
            mc.gameMode.attack(attacker, target);
            mc.player.swing(hand);
            return;
        }

        if (viaInstance.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            mc.player.swing(hand);
            mc.gameMode.attack(attacker, target);
        } else {
            mc.gameMode.attack(attacker, target);
            mc.player.swing(hand); // Аналогично
        }
    }

    // Если вы хотите сохранить оригинальную сигнатуру sendFixedAttack и всегда использовать MAIN_HAND:
    /*
    public static void sendFixedAttack(Player attacker, Entity target) {
        sendFixedAttack(attacker, target, InteractionHand.MAIN_HAND);
    }
    */
}