package ca.favro.vega.common.integrations.civmodern;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.mixin.mixins.CivModernPlayerWaypointsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CivModernIntegration {
    private final Minecraft minecraft;
    private final AbstractCivModernMod civModernMod;
    private final Vega vega;
    private final Runnable syncRunnable;
    private final boolean map;
    private ScheduledExecutorService scheduledExecutorService;

    public CivModernIntegration(Vega vega, Minecraft minecraft, boolean map) {
        this.vega = vega;
        this.minecraft = minecraft;
        this.map = map;
        this.civModernMod = AbstractCivModernMod.getInstance();
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                sync();
            }
        };
        vega.LOGGER.info("CivModern integration enabled " + (map ? "with" : "without") + " minimap");
    }

    public void start() {
        if (map) {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(syncRunnable, 0, 1, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (map) {
            clearManagedWaypoints();
            if (scheduledExecutorService != null) {
                scheduledExecutorService.close();
            }
        }
    }

    public void clearManagedWaypoints() {
        if (map) {
            minecraft.execute(() -> {
                civModernMod.getWorldListener().getPlayerWaypoints().getWaypoints().removeIf((pwp) ->
                        pwp.playerName().startsWith("§v§e§g§a§r")
                );
            });
        }
    }

    public void sync() {
        if (map) {
            if (!vega.config.isShowOnMap()) {
                clearManagedWaypoints();
                return;
            }
            Entity entity = Minecraft.getInstance().getCameraEntity();
            if (entity == null) return;
            clearManagedWaypoints();
            minecraft.execute(() -> sync(entity));
        }
    }

    private void sync(Entity entity) {
        if (!vega.config.isShowOnMap()) return;
        if (map) {
            vega.getVegaWaypointManager().forEachWaypoint(vegaPlayerWaypoint -> {
                // TODO I do this check a lot. Make it a method
                if (!vegaPlayerWaypoint.id().equals(entity.getUUID())
                        && (((Instant.now().toEpochMilli() - vegaPlayerWaypoint.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())) {
                    Vec3 position = vegaPlayerWaypoint.position();
                    PlayerInfo player = null;
                    Identifier skin = null;
                    for (PlayerInfo info : minecraft.player.connection.getOnlinePlayers()) {
                        String name = info.getProfile().name();
                        if (!name.equals(vegaPlayerWaypoint.getName())) {
                            continue;
                        }
                        player = info;
                    }
                    if (player != null) {
                        skin = player.getSkin().body().texturePath();
                    } else {
                        skin = DefaultPlayerSkin.getDefaultTexture();
                    }

                    PlayerWaypoint waypoint = new PlayerWaypoint(
                            "§v§e§g§a§r " + vegaPlayerWaypoint.getName(),
                            vegaPlayerWaypoint.id(),
                            (int) position.x, (int) position.y, (int) position.z,
                            skin,
                            Instant.ofEpochMilli(vegaPlayerWaypoint.getDateAdded())
                    );
                    ((CivModernPlayerWaypointsAccessor) civModernMod.getWorldListener().getPlayerWaypoints()).vega$getWaypointsMap().put(vegaPlayerWaypoint.id(), waypoint);
                }
            }, vegaPlayerWaypoint -> minecraft.level.dimension().identifier().getPath().equals(vegaPlayerWaypoint.getWorld())
                    && Objects.equals(vega.getCurrentServerString(), vegaPlayerWaypoint.getServer()));
        }
    }
}
