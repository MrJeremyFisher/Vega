package ca.favro.vega.common.waypoint;

import ca.favro.vega.common.Vega;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VegaWaypointManager {
    private final Map<UUID, VegaPlayerWaypoint> waypoints = new ConcurrentHashMap<>();
    private final Logger LOGGER = Vega.getInstance().LOGGER;

    public void trackOrUpdate(VegaPlayerWaypoint trackedWaypoint) {
        if (waypoints.containsKey(trackedWaypoint.id())) {
            updateWaypoint(trackedWaypoint);
        } else {
//            LOGGER.info("Tracking new waypoint for {} with id {} in {}", trackedWaypoint.getName(), trackedWaypoint.id().toString(), trackedWaypoint.getWorld());
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

    public void untrackWaypoint(UUID id) {
        this.waypoints.remove(id);
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

    public void forEachWaypoint(Consumer<VegaPlayerWaypoint> action, Predicate<VegaPlayerWaypoint> filter) {
        this.waypoints.values().stream().filter(filter).forEach(action);
    }

    public void forEachWaypointSorted(Consumer<VegaPlayerWaypoint> action, Predicate<VegaPlayerWaypoint> filter) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        this.waypoints
                .values()
                .stream()
                .sorted(Comparator.<VegaPlayerWaypoint>comparingDouble(wp -> wp.distanceSquared(entity)).reversed())
                .filter(filter).forEach(action);
    }
}
