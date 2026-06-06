package net.fabricmc.fabric.impl.base.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.MapMaker;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;

public final class EventFactoryImpl {
	private static final Set<ArrayBackedEvent<?>> ARRAY_BACKED_EVENTS
			= Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

	private EventFactoryImpl() { }

	public static void invalidate() {
		ARRAY_BACKED_EVENTS.forEach(ArrayBackedEvent::update);
	}

	public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
		ArrayBackedEvent<T> event = new ArrayBackedEvent<>(type, invokerFactory);
		ARRAY_BACKED_EVENTS.add(event);
		return event;
	}

	public static void ensureContainsDefault(Identifier[] defaultPhases) {
		for (Identifier id : defaultPhases) {
			if (id.equals(Event.DEFAULT_PHASE)) {
				return;
			}
		}

		throw new IllegalArgumentException("The event phases must contain Event.DEFAULT_PHASE.");
	}

	public static void ensureNoDuplicates(Identifier[] defaultPhases) {
		for (int i = 0; i < defaultPhases.length; ++i) {
			for (int j = i+1; j < defaultPhases.length; ++j) {
				if (defaultPhases[i].equals(defaultPhases[j])) {
					throw new IllegalArgumentException("Duplicate event phase: " + defaultPhases[i]);
				}
			}
		}
	}

	private static <T> T buildEmptyInvoker(Class<T> handlerClass, Function<T[], T> invokerSetup) {
		Method funcIfMethod = null;

		for (Method m : handlerClass.getMethods()) {
			if ((m.getModifiers() & (Modifier.STRICT | Modifier.PRIVATE)) == 0) {
				if (funcIfMethod != null) {
					throw new IllegalStateException("Multiple virtual methods in " + handlerClass + "; cannot build empty invoker!");
				}

				funcIfMethod = m;
			}
		}

		if (funcIfMethod == null) {
			throw new IllegalStateException("No virtual methods in " + handlerClass + "; cannot build empty invoker!");
		}

		Object defValue = null;

		try {
			MethodHandle target = MethodHandles.lookup().unreflect(funcIfMethod);
			MethodType type = target.type().dropParameterTypes(0, 1);

			if (type.returnType() != void.class) {
				MethodType objTargetType = MethodType.genericMethodType(type.parameterCount()).changeReturnType(type.returnType()).insertParameterTypes(0, target.type().parameterType(0));
				MethodHandle objTarget = MethodHandles.explicitCastArguments(target, objTargetType);

				Object[] args = new Object[target.type().parameterCount()];
				args[0] = invokerSetup.apply((T[]) Array.newInstance(handlerClass, 0));

				defValue = objTarget.invokeWithArguments(args);
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

		final Object returnValue = defValue;
		//noinspection unchecked
		return (T) Proxy.newProxyInstance(EventFactoryImpl.class.getClassLoader(), new Class[]{handlerClass},
			(proxy, method, args) -> returnValue);
	}
}
