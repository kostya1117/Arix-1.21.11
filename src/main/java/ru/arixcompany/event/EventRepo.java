package ru.arixcompany.event;

import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventRepo {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<>();

    public static void register(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (!isMethodBad(method)) {
                register(method, object);
            }
        }
    }

    public static void unregister(Object object) {
        for (List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.getSource().equals(object));
        }
        cleanMap(true);
    }

    private static void register(Method method, Object object) {
        try {
            Class<? extends Event> indexClass = (Class<? extends Event>) method.getParameterTypes()[0];
            MethodData data = new MethodData(object, method);

            if (!data.getTarget().isAccessible()) {
                data.getTarget().setAccessible(true);
            }

            if (REGISTRY_MAP.containsKey(indexClass)) {
                if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                    REGISTRY_MAP.get(indexClass).add(data);
                }
            } else {
                REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<>() {{
                    add(data);
                }});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        Iterator<Entry<Class<? extends Event>, List<MethodData>>> it = REGISTRY_MAP.entrySet().iterator();
        while (it.hasNext()) {
            if (!onlyEmptyEntries || it.next().getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1
                || !method.isAnnotationPresent(EventHandler.class);
    }

    public static Event call(Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());
        if (dataList != null) {
            for (MethodData data : dataList) {
                invoke(data, event);
            }
        }
        return event;
    }

    private static void invoke(MethodData data, Event argument) {
        try {
            data.getTarget().invoke(data.getSource(), argument);
        } catch (IllegalArgumentException | IllegalAccessException ignored) {
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) cause.printStackTrace();
        }
    }

    @Getter
    private static final class MethodData {
        private final Object source;
        private final Method target;

        public MethodData(Object source, Method target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodData that = (MethodData) o;
            return source.equals(that.source) && target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }
    }
}