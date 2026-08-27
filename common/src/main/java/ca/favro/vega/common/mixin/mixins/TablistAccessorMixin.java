package ca.favro.vega.common.mixin.mixins;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PlayerTabOverlay.class)
public interface TablistAccessorMixin {
    @Accessor(
            value = "header"
    )
    Component vega$getHeader();

    @Accessor(
            value = "footer"
    )
    Component vega$getFooter();
}
