package ca.favro.vega.common.websocket;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.waypoint.VegaPlayer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class VegaWebsocketHandler implements WebSocket.Listener {
    private final Logger LOGGER;
    private final StringBuffer messageBuffer = new StringBuffer();
    private final Vega vega;
    private long start = 0;

    Type vegaUserListType = new TypeToken<ArrayList<VegaUser>>() {
    }.getType();

    public VegaWebsocketHandler(Vega vega) {
        this.LOGGER = vega.LOGGER;
        this.vega = vega;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        start = Instant.now().getEpochSecond();
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
                                       int statusCode,
                                       String reason) {
        Vega.popCloseToast();
        String ret = String.format("Websocket connection closed with code %s,", statusCode);
        if (reason != null && !reason.isBlank()) {
            ret += String.format(" \"%s\",", reason);
        }

        ret += String.format(" lifetime of %s seconds", Instant.now().getEpochSecond() - start);
        LOGGER.info(ret);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (!vega.config.isReceiveInfo()) {
            return WebSocket.Listener.super.onText(webSocket, data, true);
        }

        messageBuffer.append(data);
        if (!last) {
            return WebSocket.Listener.super.onText(webSocket, data, false);
        } else {
            String incoming = messageBuffer.toString();
            messageBuffer.delete(0, messageBuffer.length());
            if (data.length() == 3) {
                if (data.toString().equals("200")) {
                    LOGGER.info("Connected to Vega server at {}", vega.config.getWssURL());
                    Vega.popOpenToast();
                    return WebSocket.Listener.super.onText(webSocket, data, true);
                } else if (data.toString().equals("401")) {
                    LOGGER.info("Vega server {} connection refused. Are you authenticated?", vega.config.getWssURL());
                    return WebSocket.Listener.super.onText(webSocket, data, true);
                }
                return WebSocket.Listener.super.onText(webSocket, data, true);
            }
            String[] incomingQuery = incoming.split("\\?");

            if (incomingQuery[0].equals("player")) {
                try {
                    VegaPlayer receivedPlayer = Vega.gson.fromJson(incomingQuery[1], VegaPlayer.class);
                    vega.receiveRemotePlayer(receivedPlayer);
                } catch (JsonSyntaxException e) {
                    LOGGER.error("Error deserializing remote player", e);
                }
            } else if (incomingQuery[0].equals("users")) {
                try {
                    ArrayList<VegaUser> receivedUsers = Vega.gson.fromJson(incomingQuery[1], vegaUserListType);
                    Map<UUID, VegaUser> hashMap = new ConcurrentHashMap<>(receivedUsers.size());
                    for (VegaUser user : receivedUsers) {
                        hashMap.put(user.uuid(), user);
                    }
                    Vega.getInstance().setVegaUsers(hashMap);
                } catch (JsonSyntaxException e) {
                    LOGGER.error("Error deserializing user update", e);
                }
            } else {
                LOGGER.warn("Received unknown packet:\n {}", incoming);
            }
            return WebSocket.Listener.super.onText(webSocket, data, true);
        }
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket,
                                      ByteBuffer message) {
        webSocket.sendPong(message);
        return WebSocket.Listener.super.onPing(webSocket, message);
    }


    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.error(error.getMessage(), error);
        Vega.popCloseToast();
        // TODO auto reconnect
    }
}
