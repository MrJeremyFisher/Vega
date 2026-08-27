package ca.favro.vega.common.mixin.mixins;

import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.WaypointContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
        value = WaypointManager.class,
        remap = false
)
public interface VoxelWaypointManagerAccessor {
    @Accessor(
            value = "waypointContainer",
            remap = false
    )
    WaypointContainer vega$getWaypointContainer();
}
