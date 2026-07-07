package ca.favro.vega.common.renderers;

import ca.favro.vega.common.waypoint.VegaPlayerWaypoint;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static int status2Color(int status) {
        return switch (status) {
            case 1 -> 0xFF00FF00;
            case 2 -> 0xFFFFDF00;
            case 3 -> 0xFFFF0000;
            default -> 0xFFFFFFFF;
        };
    }

    public static String getWaypointTimeString(VegaPlayerWaypoint vegaPlayerWaypoint) {
        String string = "";
        long deltaTime = Instant.now().toEpochMilli() - vegaPlayerWaypoint.getDateAdded();
        long hrs = TimeUnit.MILLISECONDS.toHours(deltaTime) % 24;
        long min = TimeUnit.MILLISECONDS.toMinutes(deltaTime) % 60;
        long sec = TimeUnit.MILLISECONDS.toSeconds(deltaTime) % 60;
        if (hrs > 0) {
            string += String.format(" (%dh ago)", hrs);
        } else if (min > 0) {
            string += String.format(" (%dm ago)", min);
        } else if (sec > 0) {
            string += String.format(" (%ds ago)", sec);
        }
        return string;
    }
}
