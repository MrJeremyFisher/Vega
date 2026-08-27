package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.common.Vega;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoints;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PlayerWaypoints.class)
public class CivModernPlayerWaypointsMixin {
    @Inject(
            method = "acceptSnitchHit(Lnet/minecraft/network/chat/Component;)V",
            at = @At(value = "HEAD"),
            remap = false,
            cancellable = true
    )
    private void vega$acceptorBlocker(Component message, CallbackInfo ci) {
        if (Vega.getInstance().config.isRenderEnabled()) {
            ci.cancel();
        }
    }
}
