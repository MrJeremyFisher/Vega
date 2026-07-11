package ca.favro.vega.common.waypoint;

import ca.favro.vega.common.Vega;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VegaWaypointManager {
    private final Map<UUID, VegaPlayerWaypoint> waypoints = new ConcurrentHashMap<>();
    private final Logger LOGGER = Vega.getInstance().LOGGER;

    public void trackOrUpdate(VegaPlayerWaypoint trackedWaypoint) {
        if (waypoints.containsKey(trackedWaypoint.id())) {
            updateWaypoint(trackedWaypoint);
        } else {
            LOGGER.info("Tracking new waypoint for {} with id {} in {}", trackedWaypoint.getName(), trackedWaypoint.id().toString(), trackedWaypoint.getWorld());
            trackWaypoint(trackedWaypoint);
        }
    }

    public void trackWaypoint(VegaPlayerWaypoint trackedWaypoint) {
        this.waypoints.put(trackedWaypoint.id(), trackedWaypoint);
    }

    public void updateWaypoint(VegaPlayerWaypoint trackedWaypoint) {
        this.waypoints.get(trackedWaypoint.id()).update(trackedWaypoint);
    }

    public void untrackWaypoint(VegaPlayerWaypoint trackedWaypoint) {
        this.waypoints.remove(trackedWaypoint.id());
    }

    public void untrackAllWaypoints() {
        forEachWaypoint(this::untrackWaypoint);
    }

    public boolean hasWaypoints() {
        return !this.waypoints.isEmpty();
    }

    public void forEachWaypoint(Consumer<VegaPlayerWaypoint> action) {
        this.waypoints.values().forEach(action);
    }

    public void forEachWaypoint(Consumer<VegaPlayerWaypoint> action, String world) {
        this.waypoints.values().stream().filter(e-> Objects.equals(e.getWorld(), world)).forEach(action);
    }
}
