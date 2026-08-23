package ca.favro.vega.common.mixin.mixins;

import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.WaypointContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
        value = {WaypointContainer.class},
        remap = false
)
public interface VoxelWaypointContainerAccessorMixin {
    @Accessor(
            value = "waypointManager",
            remap = false
    )
    WaypointManager getWaypointManager();
}