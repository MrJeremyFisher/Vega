package ca.favro.vega.common.mixin.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sh.okx.civmodern.common.map.Minimap;

@Mixin(Minimap.class)
public class CivModernMinimapMixin {
    @ModifyArgs(method = "onRender(Lsh/okx/civmodern/common/events/PostRenderGameOverlayEvent;)V",
            at = @At(value = "INVOKE",
                    target = "Lsh/okx/civmodern/common/map/waypoints/PlayerWaypoint;render(Lnet/minecraft/client/gui/GuiGraphics;I)V"
            ),
            remap = false)
    private void vega$reColourIcons(Args args) {
        args.set(1, 0xFFFFFFFF);
    }
}
