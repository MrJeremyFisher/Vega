package ca.favro.vega.common.integrations.journeymap;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.renderers.Utils;
import journeymap.api.client.impl.ClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.client.waypoint.ClientWaypointImpl;
import journeymap.common.waypoint.WaypointGroupStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JourneymapIntegration {
    private final Minecraft minecraft;
    private final Vega vega;
    private final Runnable syncRunnable;
    private final ArrayList<ClientWaypointImpl> trackedPts = new ArrayList<>();
    private ScheduledExecutorService scheduledExecutorService;

    public JourneymapIntegration(Vega vega, Minecraft minecraft) {
        this.vega = vega;
        this.minecraft = minecraft;
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                sync();
            }
        };
        vega.LOGGER.info("Journeymap integration enabled");
    }

    public void start() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(syncRunnable, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        trackedPts.clear();
        clearManagedWaypoints();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.close();
        }
    }

    public void clearManagedWaypoints() {
        trackedPts.clear();
        minecraft.execute(() -> {
            ClientAPI.INSTANCE.removeAll(Vega.MOD_ID);
        });
    }

    public void sync() {
        if (!vega.config.isShowOnMap()) return;
        trackedPts.clear();
        if (!vega.config.isShowOnMap()) {
            clearManagedWaypoints();
        }
        String level = minecraft.level.dimension().identifier().getPath();
        String server = vega.getCurrentServerString();
        vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
            // TODO I do this check a lot. Make it a method
            if (!vegaPlayerWaypoint.id().equals(Minecraft.getInstance().getCameraEntity().getUUID())
                    && (((Instant.now().toEpochMilli() - vegaPlayerWaypoint.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())) {
                Vec3 position = vegaPlayerWaypoint.position();
                VegaUser vegaUser = vega.getVegaUsers().get(vegaPlayerWaypoint.id());
                int color = 0xFFFFFFFF;
                if (vegaUser != null) {
                    color = Utils.status2Color(vegaUser.status());
                }
                String ts = Utils.getWaypointTimeString(vegaPlayerWaypoint);
                Waypoint waypoint = WaypointFactory.createWaypoint(
                        Vega.MOD_ID,
                        new BlockPos(new Vec3i((int) position.x, (int) position.y, (int) position.z)),
                        vegaPlayerWaypoint.getName() + ts,
                        switch (vegaPlayerWaypoint.getWorld()) {
                            case "overworld":
                                yield Level.OVERWORLD;
                            case "the_nether":
                                yield Level.NETHER;
                            case "the_end":
                                yield Level.END;
                            default:
                                vega.LOGGER.error("Unknown world in JourneyMap waypoint {}", vegaPlayerWaypoint.getWorld());
                                yield null;
                        },
                        false
                );
                waypoint.setColor(color);


                // TODO if I ever look at loading resources in common again
//                waypoint.setIconResourceLoctaion(Identifier.fromNamespaceAndPath(
//                        "journeymap", "textures/waypoint/icon/" +
//                                switch (vega.getTrackedPlayers().get(vegaPlayerWaypoint.id()).source()) {
//                                    case LOCAL -> "person.png";
//                                    case REMOTE -> "world";
//                                    case SNITCH -> "camera";
//                                    case null -> "point";
//                                }
//                ));
                trackedPts.add((ClientWaypointImpl) waypoint);
            }
        }, vegaPlayerWaypoint -> level.equals(vegaPlayerWaypoint.getWorld())
                && Objects.equals(server, vegaPlayerWaypoint.getServer()));
    }

    public ArrayList<ClientWaypointImpl> getExtraJourneymapWaypoints() {
        return trackedPts;
    }
}
