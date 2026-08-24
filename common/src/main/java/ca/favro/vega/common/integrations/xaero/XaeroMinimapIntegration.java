package ca.favro.vega.common.integrations.xaero;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.renderers.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class XaeroMinimapIntegration {
    private final Minecraft minecraft;
    private final Vega vega;
    private final Runnable syncRunnable;
    private ScheduledExecutorService scheduledExecutorService;

    public XaeroMinimapIntegration(Vega vega, Minecraft minecraft) {
        this.vega = vega;
        this.minecraft = minecraft;
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                sync();
            }
        };
        vega.LOGGER.info("Xaero's Minimap integration enabled");
    }

    public void start() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(syncRunnable, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        clearManagedWaypoints();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.close();
        }
    }

    public void clearManagedWaypoints() {
        minecraft.execute(() -> {
            MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
            MinimapWorld world = minimapSession.getWorldManager().getAutoWorld();
            if (world == null) return;
            WaypointSet currentSet = world.getCurrentWaypointSet();
            Set<Waypoint> toRemove = new HashSet<>();
            currentSet.getWaypoints().forEach(
                    waypoint -> {
                        if (waypoint.getName().startsWith("§v§e§g§a")) {
                            toRemove.add(waypoint);
                        }
                    }
            );
            currentSet.removeAll(toRemove);
        });
    }

    public void sync() {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        // This is stupid
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        MinimapWorld world = minimapSession.getWorldManager().getAutoWorld();
        if (world == null) return;
        WaypointSet currentSet = world.getCurrentWaypointSet();

        clearManagedWaypoints();
        minecraft.execute(() -> sync(entity, currentSet));
    }

    private void sync(Entity entity, WaypointSet currentSet) {
        vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
            // TODO I do this check a lot. Make it a method
            if (!vegaPlayerWaypoint.id().equals(entity.getUUID())
                    && (((Instant.now().toEpochMilli() - vegaPlayerWaypoint.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())) {
                Vec3 position = vegaPlayerWaypoint.position();
                VegaUser vegaUser = vega.getVegaUsers().get(vegaPlayerWaypoint.id());
                int status = 0;
                if (vegaUser != null) {
                    status = vegaUser.status();
                }
                String ts = Utils.getWaypointTimeString(vegaPlayerWaypoint);
                currentSet.add(new xaero.common.minimap.waypoints.Waypoint(
                        (int) position.x,
                        (int) position.y,
                        (int) position.z,
                        "§v§e§g§a§r " + vegaPlayerWaypoint.getName() + ts,
                        vegaPlayerWaypoint.getName().substring(0, 1),
                        switch (status) {
                            case 1 -> WaypointColor.GREEN;
                            case 2 -> WaypointColor.YELLOW;
                            case 3 -> WaypointColor.RED;
                            default -> WaypointColor.WHITE;
                        },
                        WaypointPurpose.NORMAL,
                        true
                ));
            }
        }, vegaPlayerWaypoint -> minecraft.level.dimension().identifier().getPath().equals(vegaPlayerWaypoint.getWorld())
                && Objects.equals(vega.getCurrentServerString(), vegaPlayerWaypoint.getServer()));
    }
}
