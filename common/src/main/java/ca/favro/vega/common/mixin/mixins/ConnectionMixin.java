package ca.favro.vega.common.mixin.mixins;

import ca.favro.vega.client.VegaFabric;
import ca.favro.vega.common.Vega;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public class ConnectionMixin {
    @Inject(
            at = {@At("HEAD")},
            method = {"send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V"},
            cancellable = true
    )
    private void sendPacket(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        try {
            boolean dropPacket = Vega.getInstance().handlePacketSending(packet);
            if (dropPacket) {
                ci.cancel();
            }
        } catch (Throwable e) {
            Vega.getInstance().LOGGER.error(e.getMessage(), e);
        }
    }

    @Inject(
            method = {"genericsFtw"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private static void handlePacket(Packet<?> packet, PacketListener packetListener, CallbackInfo ci) {
        try {
            boolean dropPacket = VegaFabric.vega.handlePacketReceiving(packet);
            if (dropPacket) {
                ci.cancel();
            }
        } catch (Throwable e) {
            Vega.getInstance().LOGGER.error(e.getMessage(), e);
        }
    }

//    @Inject(
//            method = {"Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/DisconnectionDetails;)V"},
//            at = {@At("HEAD")}
//    )
//    private void handleDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
//        try {
////            Connection self = (Connection) (Object) this;
////            VegaFabric.vega.handleDisconnectedFromServer(disconnectionDetails, self);
//        } catch (Throwable e) {
//            Vega.getInstance().LOGGER.error(e.getMessage(), e);
//        }
//    }
}