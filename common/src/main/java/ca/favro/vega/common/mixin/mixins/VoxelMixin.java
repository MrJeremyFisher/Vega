package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.common.Vega;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(
        value = {WaypointManager.class},
        remap = false
)
public abstract class VoxelMixin {
    @Shadow
    private ArrayList<Waypoint> wayPts;

    @Inject(
            method = {"getWaypoints"},
            at = {@At("HEAD")},
            cancellable = true,
            remap = false
    )
    public void vega$getWaypoints(CallbackInfoReturnable<ArrayList<Waypoint>> cir) {
        ArrayList<Waypoint> extra = Vega.getInstance().getVoxelmapIntegration().getExtraVoxelMapWaypoints();
        if (extra == null) {
            cir.setReturnValue(this.wayPts);
        } else {
            extra.addAll(this.wayPts);
            cir.setReturnValue(extra);
        }
    }

    @Inject(
            method = {"enteredDimension"},
            at = {@At("RETURN")},
            remap = false
    )
    public void vega$enteredDimension(DimensionContainer dimension, CallbackInfo ci) {
        // TODO clear waypts?
    }
}

