package ca.favro.vega.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Player;

import javax.crypto.SecretKey;
import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.UUID;
import java.util.function.Consumer;

public interface IVega {
    static String generateServerId(String baseServerId,   // Empty on vanilla servers? Is it ever not? TODO
                                   PublicKey publicKey,   // Server side public key
                                   SecretKey secretKey    // Server-client shared secret. Created on receipt of S2C hello
    ) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(baseServerId.getBytes("ISO_8859_1"));
        messageDigest.update(secretKey.getEncoded());
        messageDigest.update(publicKey.getEncoded());
        byte[] digestData = messageDigest.digest();
        return new BigInteger(digestData).toString(16);
    }

    void init();

    void handleConnectToServer(ClientPacketListener clientPacketListener);

    void handlePlayerMove(Player player);

    void handleScreenshot(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer);

    void renderOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker);

    void renderWaypointBeams(PoseStack matrices, MultiBufferSource multiBufferSource);

    void setServerHash(String s);

    String getServerHash(String s);

    boolean handlePacketReceiving(Packet<?> packet);

    boolean handlePacketSending(Packet<?> packet);

    void handleDisconnectedFromServer(DisconnectionDetails disconnectionDetails, Connection self);

    Component handleReplaceName(Component name, UUID uuid);

    void tick(Minecraft minecraft);
}
