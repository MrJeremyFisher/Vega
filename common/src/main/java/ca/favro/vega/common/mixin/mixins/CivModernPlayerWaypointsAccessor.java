package ca.favro.vega.common.mixin.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoints;

import java.util.Map;
import java.util.UUID;

@Mixin(value = PlayerWaypoints.class)
public interface CivModernPlayerWaypointsAccessor {
    @Accessor(
            value = "waypoints",
            remap = false)
    Map<UUID, PlayerWaypoint> vega$getWaypointsMap();
}
