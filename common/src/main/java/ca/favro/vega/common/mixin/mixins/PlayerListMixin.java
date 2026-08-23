package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.client.VegaFabric;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerTabOverlay.class)
public class PlayerListMixin {
    @Inject(
            method = {"getNameForDisplay"},
            at = {@At("HEAD")},
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    protected void modifyDisplayName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        try {
            Component displayName = VegaFabric.vega.handleReplaceName(playerInfo.getTabListDisplayName(), playerInfo.getProfile().id());
            if (displayName != null) {
                cir.setReturnValue(displayName);
            }
        } catch (Throwable e) {
            VegaFabric.LOGGER.error("Error in Vega tablist handling", e);
        }

    }
}
