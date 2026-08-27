package ca.favro.vega.common.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.persistent.CompressibleMapRegionTexture;
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CompressibleMapRegionTexture.class)
public class VoxelCompressibleMapRegionMixin {
    // Fixes an image not allocated error/crash. Not sure why it happens, but this fixes it.
    @Inject(method = "setRGB(III)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixel(III)V"), cancellable = true)
    public void vega$fixNotAllocCrash(int x, int y, int color, CallbackInfo ci, @Local(name = "localPixels") NativeImage localPixels) {
        if (localPixels.getPointer() == 0L) {
            ci.cancel();
        }
    }
}