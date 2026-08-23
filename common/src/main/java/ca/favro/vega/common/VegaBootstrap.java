package ca.favro.vega.common;

import ca.favro.vega.client.IVegaBootstrap;
import org.spongepowered.asm.mixin.Mixins;

public class VegaBootstrap implements IVegaBootstrap {
    public void loadMixins() {
        System.out.println("[Vega] Loading Mixins");
        Mixins.addConfiguration("vega.common.mixins.json");
        System.out.println("[Vega] Loaded Mixins");
    }
}
