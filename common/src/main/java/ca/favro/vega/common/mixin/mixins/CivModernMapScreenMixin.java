package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.renderers.Utils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sh.okx.civmodern.common.map.screen.MapScreen;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;

@Mixin(MapScreen.class)
public class CivModernMapScreenMixin {
    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
            ),
            remap = false)
    private void reColourNames(Args args, @Local PlayerWaypoint waypoint) {
        args.set(1, ((MutableComponent) args.get(1)).withColor(Utils.status2Color(Vega.getInstance().getStatus(waypoint.playerId()))));
        args.set(4, 0xFFFFFFFF);
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lsh/okx/civmodern/common/map/waypoints/PlayerWaypoint;render(Lnet/minecraft/client/gui/GuiGraphics;I)V"
            ),
            remap = false,
            index = 1)
    private int reColourIcons(int colour) {
        return 0xFFFFFFFF;
    }
}
