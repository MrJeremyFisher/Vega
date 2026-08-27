package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.client.VegaFabric;
import ca.favro.vega.common.Vega;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

@Mixin({Screenshot.class})
public class ScreenshotMixin {
    @Inject(
            method = {"grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V"},
            at = {@At("HEAD")}
    )
    private static void vega$saveScreenshot(File file, @Nullable String string, RenderTarget renderTarget, int i, Consumer<Component> consumer, CallbackInfo ci) {
        try {
            if (Vega.getInstance() != null) {
                Vega.getInstance().handleScreenshot(file, renderTarget, consumer);
            }
        } catch (Throwable e) {
            Vega.getInstance().LOGGER.error("Error in Vega screenshot handling", e);
        }
    }
}