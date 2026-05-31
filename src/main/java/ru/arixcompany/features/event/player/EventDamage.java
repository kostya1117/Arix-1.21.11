package ru.arixcompany.features.event.player;

import lombok.Getter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ru.arixcompany.features.event.Event;

@Getter
public class EventDamage extends Event {
    private final Player player;
    private final DamageSource source;
    private final float amount;

    public EventDamage(Player player, DamageSource source, float amount) {
        this.player = player;
        this.source = source;
        this.amount = amount;
    }

    public Entity attacker() {
        return source.getEntity();
    }

    public Entity direct() {
        return source.getDirectEntity();
    }

    public boolean isThorns() {
        return source.is(DamageTypes.THORNS);
    }
}
