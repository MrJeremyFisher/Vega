package ca.favro.vega.client.mixin;

import ca.favro.vega.client.VegaFabric;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({Player.class})
public class PlayerEntityMixin {

    @Inject(
            method = {"getDisplayName"},
            at = {@At("RETURN")},
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    public void modifyDisplayName(CallbackInfoReturnable<Component> cir, MutableComponent mutableComponent) {
        try {
            Component replacement = VegaFabric.vega.handleReplaceName(cir.getReturnValue(), ((Player) (Object) this).getUUID());
            if (replacement != null) {
                cir.setReturnValue(replacement);
            }
        } catch (Throwable e) {
            VegaFabric.LOGGER.error("Error in Vega nameplate handling", e);
        }

    }

}
