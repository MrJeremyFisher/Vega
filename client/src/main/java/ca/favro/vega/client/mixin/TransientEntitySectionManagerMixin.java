package ca.favro.vega.client.mixin;

import ca.favro.vega.client.VegaFabric;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TransientEntitySectionManager.class)
public class TransientEntitySectionManagerMixin {

    @Inject(at = @At("TAIL"), method = "addEntity")
    public void addEntity(EntityAccess entity, CallbackInfo ci) {
        if (!entity.isAlwaysTicking() || VegaFabric.vega == null) { // Only players are alwaysTicking
            return;
        }
        VegaFabric.vega.handlePlayerMove(((Player) entity));
    }

    @Mixin(targets = "net.minecraft.world.level.entity.TransientEntitySectionManager$Callback")
    static class ListenerMixin {
        @Final
        @Shadow
        private EntityAccess entity;

        @Inject(at = @At("TAIL"), method = "onMove")
        public void updateEntityPosition(CallbackInfo ci) {
            if (!entity.isAlwaysTicking() || VegaFabric.vega == null) { // Only players are alwaysTicking
                return;
            }
            VegaFabric.vega.handlePlayerMove(((Player) entity));
        }
    }
}