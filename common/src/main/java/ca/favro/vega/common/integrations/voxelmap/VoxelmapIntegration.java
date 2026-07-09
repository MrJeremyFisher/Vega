package ca.favro.vega.common.integrations.voxelmap;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.renderers.Utils;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VoxelmapIntegration {
    private Minecraft minecraft;
    private Vega vega;
    private Runnable syncRunnable;
    private ScheduledExecutorService scheduledExecutorService;
    private ArrayList<Waypoint> trackedPts = new ArrayList<>();

    public VoxelmapIntegration(Vega vega, Minecraft minecraft) {
        this.vega = vega;
        this.minecraft = minecraft;
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                sync();
            }
        };
        vega.LOGGER.info("Voxelmap integration enabled");
    }

    public void start() {
        VoxelConstants.getVoxelMapInstance().runAfterInitialized(() -> {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(syncRunnable, 0, 1, TimeUnit.SECONDS);
        });
    }

    public void stop() {
        clearManagedWaypoints();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.close();
        }
    }

    public void clearManagedWaypoints() {
        minecraft.execute(() -> {
            WaypointManager voxelWaypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
            for (Waypoint waypoint :
                    voxelWaypointManager.getWaypoints().stream().filter(waypoint ->
                            waypoint.name.startsWith("§v§e§g§a")).collect(Collectors.toSet())
            ) {
                // Removing this way instead of straight from the list calls refreshRenderables so the waypoints disappear from the map
                voxelWaypointManager.deleteWaypoint(waypoint);
            }
        });
    }

    public void sync() {
        if (!vega.config.isShowOnMap()) {
            clearManagedWaypoints();
            return;
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        WaypointManager voxelWaypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        if (voxelWaypointManager == null || minecraft.level == null) return;
        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
        if (entity == null) return;
        minecraft.execute(() -> sync(voxelWaypointManager, entity, dimensions));
    }

    private void sync(WaypointManager voxelWaypointManager, Entity entity, TreeSet<DimensionContainer> dimensions) {
        for (Waypoint waypoint : trackedPts) {
            voxelWaypointManager.deleteWaypoint(waypoint);
        }
        trackedPts.clear();

        vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
            // TODO I do this check a lot. Make it a method
            if (!vegaPlayerWaypoint.id().equals(entity.getUUID())
                    && (((Instant.now().toEpochMilli() - vegaPlayerWaypoint.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())) {
                Vec3 position = vegaPlayerWaypoint.position();
                VegaUser vegaUser = vega.getVegaUsers().get(vegaPlayerWaypoint.id());
                int color = 0xFFFFFFFF;
                if (vegaUser != null) {
                    color = Utils.status2Color(vegaUser.status());
                }
                String ts = Utils.getWaypointTimeString(vegaPlayerWaypoint);
                Waypoint waypoint = new Waypoint(
                        "§v§e§g§a§r " + vegaPlayerWaypoint.getName() + ts,
                        (int) position.x,
                        (int) position.z,
                        (int) position.y,
                        true,
                        (color >> 16 & 0xFF) / 255F,
                        (color >> 8 & 0xFF) / 255F,
                        (color & 0xFF) / 255F,
                        "camera", // TODO type dependent icons
                        vegaPlayerWaypoint.getWorld(),
                        dimensions
                );
                voxelWaypointManager.addWaypoint(waypoint);
                trackedPts.add(waypoint);
            }
        }, minecraft.level.dimension().identifier().getPath());
    }
}
