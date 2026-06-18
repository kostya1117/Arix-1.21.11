package ru.arixcompany.features.repos.alerts;

import lombok.Getter;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.quad.EaseInOutQuad;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AlertRepo {

    @Getter
    private static final List<NotificationEntry> notifications = new CopyOnWriteArrayList<>();

    public static void show(AlertType type, String text, long duration, boolean pinned) {
        notifications.add(new NotificationEntry(type, text, duration, pinned));
        notifications.sort(Comparator.comparingDouble(n -> n.pinned ? -1 : 1));
    }

    public static void success(String text) { show(AlertType.SUCCESS, text, 2000, false); }
    public static void error(String text) { show(AlertType.ERROR, text, 2000, false); }
    public static void info(String text) { show(AlertType.INFO, text, 2000, false); }
    public static void activate(String text) { show(AlertType.ACTIVATE, text, 1500, false); }
    public static void deactivate(String text) { show(AlertType.DEACTIVATE, text, 1500, false); }
    public static void warn(String text) { show(AlertType.WARN, text, 2000, false); }
    public static void event(String text) { show(AlertType.EVENT, text, 3000, false); }

    public static void update() {
        for (NotificationEntry entry : notifications) {
            boolean expired = (System.currentTimeMillis() - entry.createTime) > entry.duration;

            if (expired) {
                entry.animation.setDirection(Direction.BACKWARDS);
            }

            if (entry.animation.getDirection() == Direction.BACKWARDS && entry.animation.getOutput() <= 0.001f) {
                notifications.remove(entry);
            }
        }
    }

    public static class NotificationEntry {
        public final AlertType type;
        public final String text;
        public final long duration;
        public final boolean pinned;
        public final long createTime;
        public final Animation animation = new EaseInOutQuad(300, 1.0);

        public NotificationEntry(AlertType type, String text, long duration, boolean pinned) {
            this.type = type;
            this.text = text;
            this.duration = duration;
            this.pinned = pinned;
            this.createTime = System.currentTimeMillis();
            this.animation.setDirection(Direction.FORWARDS);
        }
    }
}