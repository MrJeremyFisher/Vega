package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.common.Vega;
import journeymap.client.waypoint.ClientWaypointImpl;
import journeymap.common.waypoint.WaypointStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(
        value = WaypointStore.class,
        remap = false
)
public class JourneymapWaypointStoreMixin {
    @Inject(
            method = "getAll()Ljava/util/Collection;",
            at = {@At("RETURN")},
            cancellable = true,
            remap = false
    )
    private void vega$addWaypoints(CallbackInfoReturnable<Collection<ClientWaypointImpl>> cir) {
        Collection<ClientWaypointImpl> extra = Vega.getInstance().getJourneymapIntegration().getExtraJourneymapWaypoints();
        if (extra == null) {
            cir.setReturnValue(cir.getReturnValue());
        } else {
            extra.addAll(cir.getReturnValue());
            cir.setReturnValue(extra);
        }
    }
}
