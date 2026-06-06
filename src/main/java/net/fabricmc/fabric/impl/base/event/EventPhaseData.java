package net.fabricmc.fabric.impl.base.event;

import java.lang.reflect.Array;
import java.util.Arrays;

import net.fabricmc.fabric.impl.base.toposort.SortableNode;
import net.minecraft.resources.Identifier;

class EventPhaseData<T> extends SortableNode<EventPhaseData<T>> {
	final Identifier id;
	T[] listeners;

	@SuppressWarnings("unchecked")
	EventPhaseData(Identifier id, Class<?> listenerClass) {
		this.id = id;
		this.listeners = (T[]) Array.newInstance(listenerClass, 0);
	}

	void addListener(T listener) {
		int oldLength = listeners.length;
		listeners = Arrays.copyOf(listeners, oldLength + 1);
		listeners[oldLength] = listener;
	}

	@Override
	protected String getDescription() {
		return id.toString();
	}
}
