package net.fabricmc.fabric.api.event;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public abstract class Event<T> {
	protected volatile T invoker;

	public final T invoker() {
		return invoker;
	}

	public abstract void register(T listener);

	public static final Identifier DEFAULT_PHASE = new Identifier("fabric", "default");

	public void register(Identifier phase, T listener) {
		register(listener);
	}

	public void addPhaseOrdering(Identifier firstPhase, Identifier secondPhase) {
	}
}
