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
    private ArrayList<Waypoint> trackedPts = new ArrayList<>();

    public VoxelmapIntegration(Vega vega) {
        this.vega = vega;
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                sync();
            }
        };
        vega.LOGGER.info("Voxelmap integration enabled");
    }

    public void start() {
        minecraft = Minecraft.getInstance();
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(syncRunnable, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.close();
        }
    }

    public void sync() {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        WaypointManager voxelWaypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        if (voxelWaypointManager == null || minecraft.level == null) return;
        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
        if (entity == null) return;

        for (Waypoint waypoint : trackedPts) {
            voxelWaypointManager.deleteWaypoint(waypoint);
        }
        trackedPts.clear();

        vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
            if (!vegaPlayerWaypoint.id().equals(entity.getUUID())) {
                Vec3 position = vegaPlayerWaypoint.position();
                VegaUser vegaUser = vega.getVegaUsers().get(vegaPlayerWaypoint.id());
                int color = 0xFFFFFF;
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
                        (color & 0xFF) / 255F,
                        (color >> 8 & 0xFF) / 255F,
                        (color >> 16 & 0xFF) / 255F,
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
