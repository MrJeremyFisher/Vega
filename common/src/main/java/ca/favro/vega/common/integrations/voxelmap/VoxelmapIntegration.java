package ca.favro.vega.common.integrations.voxelmap;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.mixin.mixins.VoxelWaypointManagerAccessorMixin;
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

public class VoxelmapIntegration {
    private Minecraft minecraft;
    private Vega vega;
    private Runnable syncRunnable;
    private ScheduledExecutorService scheduledExecutorService;

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
        if (scheduledExecutorService != null) {
            scheduledExecutorService.close();
        }
    }

    public void sync() {
        try {
            VoxelWaypointManagerAccessorMixin voxelWaypointManager = ((VoxelWaypointManagerAccessorMixin)VoxelConstants.getVoxelMapInstance().getWaypointManager());
            if (voxelWaypointManager != null) {
                minecraft.execute(() -> voxelWaypointManager.getWaypointContainer().refreshRenderables());
            }
        } catch (Exception ignored) {

        }
    }

    public ArrayList<Waypoint> getExtraVoxelMapWaypoints() {
        if (!vega.config.isShowOnMap()) {
            return null;
        }
        Entity entity = Minecraft.getInstance().getCameraEntity();
        WaypointManager voxelWaypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        if (voxelWaypointManager == null || minecraft.level == null) return null;
        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
        if (entity == null) return null;
        ArrayList<Waypoint> trackedPts = new ArrayList<>();
        vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
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
                        vegaPlayerWaypoint.getName() + ts,
                        (int) position.x,
                        (int) position.z,
                        (int) position.y,
                        true,
                        (color >> 16 & 0xFF) / 255F,
                        (color >> 8 & 0xFF) / 255F,
                        (color & 0xFF) / 255F,
                        switch (vega.getTrackedPlayers().get(vegaPlayerWaypoint.id()).source()) {
                            case LOCAL -> "person";
                            case REMOTE -> "world";
                            case SNITCH -> "camera";
                            case null -> "point";
                        },
                        vegaPlayerWaypoint.getWorld(),
                        dimensions
                );
                trackedPts.add(
                        waypoint
                );
            }
        }, minecraft.level.dimension().identifier().getPath());

        return trackedPts;
    }
}
