package ca.favro.vega.common.waypoint;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.renderers.Utils;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAssets;

import java.util.Optional;

public class VegaWaypointIcon extends Waypoint.Icon {
    public VegaWaypointIcon() {
        this.style = WaypointStyleAssets.DEFAULT;
    }
//
//    public VegaWaypointIcon() {
//        this.style = WaypointStyleAssets.DEFAULT;
//        this.color = switch (status) {
//            case ALLY -> Optional.of(0xFF00FF00);
//            case ENEMY -> Optional.of(0xFFFF0000);
//            case WATCH -> Optional.of(0xFFFFFF00);
//            case NEUTRAL -> Optional.of(0xFFFFFFFF);
//        };
//    }
}
