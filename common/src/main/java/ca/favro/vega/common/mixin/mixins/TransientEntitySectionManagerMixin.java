package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.common.Vega;
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
    public void vega$addEntity(EntityAccess entity, CallbackInfo ci) {
        if (!entity.isAlwaysTicking() || Vega.getInstance() == null) { // Only players are alwaysTicking
            return;
        }
        Vega.getInstance().handlePlayerMove(((Player) entity));
    }

    @Mixin(targets = "net.minecraft.world.level.entity.TransientEntitySectionManager$Callback")
    static class ListenerMixin {
        @Final
        @Shadow
        private EntityAccess entity;

        @Inject(at = @At("TAIL"), method = "onMove")
        public void vega$updateEntityPosition(CallbackInfo ci) {
            if (!entity.isAlwaysTicking() || Vega.getInstance() == null) { // Only players are alwaysTicking
                return;
            }
            Vega.getInstance().handlePlayerMove(((Player) entity));
        }
    }
}