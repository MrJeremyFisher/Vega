package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.client.IVega;
import ca.favro.vega.client.VegaFabric;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.SecretKey;
import java.security.PublicKey;

@Mixin(ServerboundKeyPacket.class)
public class ClientHandshakeMixin {
    @Inject(method = "<init>(Ljavax/crypto/SecretKey;Ljava/security/PublicKey;[B)V", at = @At(value = "HEAD"))
    private static void onSendC2SKeyPacket(SecretKey secretKey, PublicKey publicKey, byte[] challenge, CallbackInfo ci) {
        try {
            if (VegaFabric.vega != null) {
                VegaFabric.vega.setServerHash(IVega.generateServerId("", publicKey, secretKey));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
