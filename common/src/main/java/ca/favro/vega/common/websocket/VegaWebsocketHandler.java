package ca.favro.vega.common.websocket;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.waypoint.VegaPlayer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static ca.favro.vega.common.Vega.config;

public class VegaWebsocketHandler implements WebSocket.Listener {
    private final Logger LOGGER;
    private StringBuffer messageBuffer = new StringBuffer();
    private Vega vega;

    Type vegaUserListType = new TypeToken<ArrayList<VegaUser>>() {
    }.getType();

    public VegaWebsocketHandler(Vega vega) {
        this.LOGGER = vega.LOGGER;
        this.vega = vega;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (!config.isReceiveInfo()) {
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
                    LOGGER.info("Connected to Vega server at {}", config.getWssURL());
                    Vega.popOpenToast();
                    return WebSocket.Listener.super.onText(webSocket, data, true);
                } else if (data.toString().equals("401")) {
                    LOGGER.info("Vega server {} connection refused. Are you authenticated?", config.getWssURL());
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
    public void onError(WebSocket webSocket, Throwable error) {
        Vega.popCloseToast();
        LOGGER.error(error.getMessage(), error);
        // TODO auto reconnect
    }
}
