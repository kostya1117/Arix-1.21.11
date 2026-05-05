package ru.arixcompany.features.repos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CopyOnWriteArrayList;

public class WayPointRepo {
    @Getter
    private static CopyOnWriteArrayList<WayPoint> wayPoints = new CopyOnWriteArrayList<>();

    public static void addWayPoint(WayPoint wp) {
        if (!wayPoints.contains(wp))
            wayPoints.add(wp);
    }

    public static void removeWayPoint(WayPoint macro) {
        wayPoints.remove(macro);
    }

    public static WayPoint getWayPointByName(String name) {
        for (WayPoint wayPoint : getWayPoints())
            if (wayPoint.name.equalsIgnoreCase(name))
                return wayPoint;
        return null;
    }


    @AllArgsConstructor
    @Setter
    @Getter
    public static class WayPoint {
        private int x, y, z;
        private String name, server;
    }
}
