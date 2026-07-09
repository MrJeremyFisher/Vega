package ca.favro.vega.common.waypoint;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.time.Instant;
import java.util.UUID;

public class VegaPlayerWaypoint implements Waypoint {
    private String name;
    private UUID identifier;
    private Vec3 vector;
    private String world;
    private long dateAdded;
    private Icon icon;

    public VegaPlayerWaypoint(UUID uuid, String world, Icon icon, Vec3 vector) {
        this.vector = vector;
        this.world = world;
        this.identifier = uuid;
        this.icon = icon;
        this.dateAdded = Instant.now().toEpochMilli();
    }

    public VegaPlayerWaypoint(VegaPlayerWaypoint vegaPlayerWaypoint) {
        this.name = vegaPlayerWaypoint.name;
        this.identifier = vegaPlayerWaypoint.identifier;
        this.world = vegaPlayerWaypoint.world;
        this.icon = vegaPlayerWaypoint.icon;
        this.vector = vegaPlayerWaypoint.vector;
        this.dateAdded = vegaPlayerWaypoint.dateAdded;
    }

    public VegaPlayerWaypoint(VegaPlayer vp) {
        this.vector = vp.position();
        this.world = vp.world();
        this.identifier = vp.uuid();
        this.name = vp.name();
        this.icon = new VegaWaypointIcon();
        this.dateAdded = vp.time();
    }

    public void update(VegaPlayerWaypoint waypoint) {
        this.vector = waypoint.vector;
        this.dateAdded = waypoint.dateAdded;
    }

    public double yawAngleToCamera(TrackedWaypoint.Camera camera) {
        Vec3 vec3 = camera.position().subtract(this.position()).rotateClockwise90();
        float f = (float) Mth.atan2(vec3.z(), vec3.x()) * (180.0F / (float) Math.PI);
        return Mth.degreesDifference(camera.yaw(), f);
    }

    public TrackedWaypoint.PitchDirection pitchDirectionToCamera(TrackedWaypoint.Projector projector) {
        Vec3 vec3 = projector.projectPointToScreen(this.position());
        boolean bl = vec3.z > 1.0;
        double d = bl ? -vec3.y : vec3.y;
        if (d < -1.0) {
            return TrackedWaypoint.PitchDirection.DOWN;
        } else if (d > 1.0) {
            return TrackedWaypoint.PitchDirection.UP;
        } else {
            if (bl) {
                if (vec3.y > 0.0) {
                    return TrackedWaypoint.PitchDirection.UP;
                }

                if (vec3.y < 0.0) {
                    return TrackedWaypoint.PitchDirection.DOWN;
                }
            }

            return TrackedWaypoint.PitchDirection.NONE;
        }
    }

    public double distanceSquared(Entity entity) {
        return entity.distanceToSqr(this.vector);
    }

    public String getName() {
        return name;
    }

    public UUID id() {
        return this.identifier;
    }

    public Icon icon() {
        return this.icon;
    }

    public Vec3 position() {
        return this.vector;
    }

    public void setPosition(Vec3 vector) {
        this.vector = vector;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public long getDateAdded() {
        return dateAdded;
    }
}
