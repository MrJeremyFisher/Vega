package ca.favro.vega.common;

import ca.favro.vega.client.IVega;
import ca.favro.vega.common.config.VegaConfig;
import ca.favro.vega.common.gui.components.IconToast;
import ca.favro.vega.common.gui.screens.SettingsScreen;
import ca.favro.vega.common.gui.screens.VegaPlayerScreen;
import ca.favro.vega.common.integrations.combatradar.CombatRadarIntegration;
import ca.favro.vega.common.integrations.voxelmap.VoxelmapIntegration;
import ca.favro.vega.common.renderers.PlayerLocationBarRenderer;
import ca.favro.vega.common.renderers.PlayerLocationBeamRenderer;
import ca.favro.vega.common.renderers.Utils;
import ca.favro.vega.common.waypoint.SnitchAlert;
import ca.favro.vega.common.waypoint.VegaPlayer;
import ca.favro.vega.common.waypoint.VegaPlayerWaypoint;
import ca.favro.vega.common.waypoint.VegaWaypointManager;
import ca.favro.vega.common.websocket.VegaWebsocketHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.mixin.networking.client.accessor.MinecraftAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Vega implements IVega {
    public static final String MOD_ID = "vega";
    private static final String MOD_VERSION = "1.0.4-1.21.11-ALPHA";
    public final Logger LOGGER;
    private static Vega instance = null;
    private String serverHash;
    private static WebSocket webSocket;
    private Set<UUID> focusedPlayers = new HashSet<>();
    private Map<UUID, VegaPlayer> trackedPlayers = new ConcurrentHashMap<>();
    private Map<UUID, VegaUser> vegaUsers = new ConcurrentHashMap<>();
    private Map<UUID, SnitchAlert> queuedSnitchAlerts = new ConcurrentHashMap<>();
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(VegaPlayer.class, new VegaPlayer.VegaPlayerSerializer())
            .registerTypeAdapter(VegaPlayer.class, new VegaPlayer.VegaPlayerDeserializer())
            .registerTypeAdapter(VegaUser.class, new VegaUser.VegaUserSerializer())
            .create();
    public VegaConfig config;
    private VegaWaypointManager vegaWaypointManager;
    private static PlayerLocationBarRenderer playerLocationBarRenderer;
    private static PlayerLocationBeamRenderer playerLocationBeamRenderer;
    private static Runnable playerSender;
    private static ScheduledExecutorService playerSenderRunner;
    private KeyMapping settingsKey;
    private KeyMapping renderKey;
    private KeyMapping listKey;
    private boolean combatRadarEnabled;
    private boolean voxelmapEnabled;
    private VoxelmapIntegration voxelmapIntegration;
    private boolean journeymapEnabled;
    private boolean xaerosmapEnabled;
    private static Pattern oldWorldKey = Pattern.compile("ResourceKey\\[minecraft:dimension \\/ minecraft:(.*)\\]");

    // TODO combat loggers have different UUIDS than the player which can cause duplicate waypoints. Could try fixing this on Civ. CTP just assigns a random UUID
    // On the server we map by name so its nbd really. Maybe we should just map by name here as well
    public Vega(KeyMapping renderKey, KeyMapping listKey, KeyMapping settingsKey, Logger logger) {
        instance = this;
        this.settingsKey = settingsKey;
        this.listKey = listKey;
        this.renderKey = renderKey;
        this.LOGGER = logger;
        // TODO does this still work if mod loads after vega? do we need to depend?
        this.combatRadarEnabled = FabricLoader.getInstance().isModLoaded("combatradar");
        this.voxelmapEnabled = FabricLoader.getInstance().isModLoaded("voxelmap");

        if (voxelmapEnabled) {
            voxelmapIntegration = new VoxelmapIntegration(this, Minecraft.getInstance());
        }
    }

    public static void popOpenToast() {
        Minecraft.getInstance().getToastManager().addToast(
                new IconToast(
                        Component.literal("Connected to Vega server!"),
                        CommonComponents.EMPTY,
                        Identifier.withDefaultNamespace("icon/link"),
                        2000L
                ));
        // TODO clear disconnect toast
    }

    public static void popCloseToast() {
        Minecraft.getInstance().getToastManager().addToast(
                new IconToast(
                        Component.literal("Disconnect from Vega server!"),
                        CommonComponents.EMPTY,
                        Identifier.withDefaultNamespace("player_list/remove_player"),
                        10000L
                ));
    }

    public Map<UUID, VegaUser> getVegaUsers() {
        return vegaUsers;
    }

    public void setVegaUsers(Map<UUID, VegaUser> vegaUsers) {
        this.vegaUsers = vegaUsers;
        if (this.combatRadarEnabled) {
            CombatRadarIntegration.syncStatuses();
        }
        if (this.voxelmapEnabled) {
            voxelmapIntegration.sync();
        }
    }

    public Set<UUID> getFocusedPlayers() {
        return focusedPlayers;
    }

    @Override
    public void init() {
        LOGGER.info("Vega client version {}", MOD_VERSION);

        File gameDirectory = Minecraft.getInstance().gameDirectory;
        File configDir = new File(gameDirectory, "/config/");
        if (!configDir.isDirectory())
            configDir.mkdir();

        File configFile = new File(configDir, "vega.json");

        config = new VegaConfig(configFile);
        if (!configFile.isFile()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            config.save();
        } else {
            if (!config.load())
                config.save();
        }

        vegaWaypointManager = new VegaWaypointManager();
        playerLocationBarRenderer = new PlayerLocationBarRenderer(this);
        playerLocationBeamRenderer = new PlayerLocationBeamRenderer(this);

        playerSender = new Runnable() {
            public void run() {
                // Filter for non local players (illegal to share them) who aren't the player running the mod, who can share their information fine
                for (VegaPlayer player : trackedPlayers.values().stream().filter(
                        player -> player.source() != VegaPlayer.Source.LOCAL || !Objects.equals(player.name(), Minecraft.getInstance().player.getGameProfile().name())
                ).collect(Collectors.toSet())

                ) {
                    if (webSocket != null && config.isSendInfo()) {
                        webSocket.sendText("?player=" + gson.toJson(player), true);
                    }
                }

                // We sync snitch hits separately as when we have someone in render range that overrides the snitch hits locally and so won't share it
                // to other users
                for (UUID snitchAlertUUID : queuedSnitchAlerts.keySet()) {
                    SnitchAlert sa = queuedSnitchAlerts.get(snitchAlertUUID);
                    if (webSocket != null && config.isSendInfo()) {
                        webSocket.sendText("?player=" + gson.toJson(new VegaPlayer(
                                sa.accountName, snitchAlertUUID, sa.pos, sa.world, sa.ts, VegaPlayer.Source.SNITCH
                        )), true);
                    }
                }
                // Reset the "queue"
                queuedSnitchAlerts.clear();
            }
        };
    }

    public static Vega getInstance() {
        return instance;
    }

    public void handleConnectToServer(ClientPacketListener clientPacketListener) {
        // TODO check connection type?
        String addr = clientPacketListener.getConnection().getRemoteAddress().toString();
        if (addr.contains("23.163.152.211") || addr.contains("play.civmc.net")) {
            LOGGER.debug("Connected to server");
            tryWSConnection();
            playerSenderRunner = Executors.newScheduledThreadPool(1);
            playerSenderRunner.scheduleAtFixedRate(playerSender, 0, 3, TimeUnit.SECONDS);
        } else {
            handleDisconnectedFromServer(null, null);
        }
    }

    @Override
    public void handleDisconnectedFromServer(DisconnectionDetails disconnectionDetails, Connection self) {
        // TODO check connection type? - I think only needed when using the mixin which fires a bunch. For now we'll use the fabric event.
        if (playerSenderRunner != null) {
            playerSenderRunner.close();
        }

        playerSender.run();
        trackedPlayers.clear();
        vegaUsers.clear();
        vegaWaypointManager.untrackAllWaypoints();

        if (webSocket != null) {
            webSocket.sendClose(400, "Client disconnected");
            webSocket.abort();
        }

        if (voxelmapEnabled) {
            voxelmapIntegration.stop();
        }
    }

    @Override
    public Component handleReplaceName(Component name, UUID uuid) {
        VegaUser vegaUser = this.vegaUsers.get(uuid);
        if (vegaUser != null && name != null) {
            return name.copy().withColor(Utils.status2Color(vegaUser.status()));
        }
        return null;
    }

    public void handlePlayerMove(Player player) {
        Vec3 pos = player.position();
        Entity e = Minecraft.getInstance().getCameraEntity();
        pos = new Vec3(pos.x, e == null ? 64 : e.getY(), pos.z);
        trackedPlayers.put(player.getUUID(),
                new VegaPlayer(player.getName().getString(),
                        player.getUUID(),
                        pos,
                        Minecraft.getInstance().level.dimension().identifier().getPath(),
                        System.currentTimeMillis(), VegaPlayer.Source.LOCAL)
        );
        vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                trackedPlayers.get(player.getUUID())
        ));
    }

    public void handleScreenshot(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {

    }

    public void receiveRemotePlayer(VegaPlayer receivedPlayer) {
        Matcher matcher = oldWorldKey.matcher(receivedPlayer.world());
        if (matcher.matches()) {
            receivedPlayer = new VegaPlayer(receivedPlayer.name(),
                    receivedPlayer.uuid(),
                    receivedPlayer.position(),
                    matcher.group(1), receivedPlayer.time(), receivedPlayer.source());
        }

        if (this.trackedPlayers.containsKey(receivedPlayer.uuid())) {
            if (this.trackedPlayers.get(receivedPlayer.uuid()).time() < receivedPlayer.time()) {
                trackedPlayers.put(receivedPlayer.uuid(),
                        receivedPlayer
                );
                vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                        trackedPlayers.get(receivedPlayer.uuid())
                ));
                if (this.voxelmapEnabled) {
                    voxelmapIntegration.sync();
                }
            }
        } else {
            trackedPlayers.put(receivedPlayer.uuid(),
                    receivedPlayer
            );
            vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                    trackedPlayers.get(receivedPlayer.uuid())
            ));
            if (this.voxelmapEnabled) {
                voxelmapIntegration.sync();
            }
        }
        // Do nothing if our entry is newer than what we got from the server
    }

    public VegaWaypointManager getVegaWaypointManager() {
        return vegaWaypointManager;
    }

    public void renderOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (config.isRenderEnabled() && config.isShowLocatorBar()) {
            playerLocationBarRenderer.render(guiGraphics, deltaTracker);
        }
    }

    public void renderWaypointBeams(PoseStack matrices, MultiBufferSource multiBufferSource) {
        if (config.isRenderEnabled()) {
            playerLocationBeamRenderer.renderWaypointBeams(matrices, multiBufferSource);
        }
    }

    @Override
    public void setServerHash(String s) {
        this.serverHash = s;
    }

    @Override
    public String getServerHash(String s) {
        return this.serverHash;
    }

    @Override
    public boolean handlePacketReceiving(Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket cscp) {
            if (Minecraft.getInstance().level == null) return false;
            SnitchAlert snitchAlert = SnitchAlert.fromChat(cscp.content(), Minecraft.getInstance().level.dimension().identifier().getPath());
            if (snitchAlert == null || Minecraft.getInstance().player == null) return false;
            Optional<PlayerInfo> o = Minecraft.getInstance().player.connection.getOnlinePlayers().stream().filter(
                    player -> Objects.equals(player.getProfile().name(),
                            snitchAlert.accountName)
            ).findFirst();
            if (o.isPresent()) {
                UUID uuid = o.get().getProfile().id();

                if (trackedPlayers.containsKey(uuid)) {
                    VegaPlayer p = trackedPlayers.get(uuid);
                    // Prefer local waypoints over snitch hits to avoid the waypoint jumping around
                    if (((Instant.now().toEpochMilli() - p.time()) / 1000) > 2 && p.source() != VegaPlayer.Source.LOCAL) {
                        handleSnitch(snitchAlert, uuid);
                    }
                } else {
                    handleSnitch(snitchAlert, uuid);
                }
            }
        }
        return false;
    }

    private void handleSnitch(SnitchAlert snitchAlert, UUID uuid) {
        trackedPlayers.put(uuid, new VegaPlayer(
                snitchAlert.accountName, uuid, snitchAlert.pos, snitchAlert.world, snitchAlert.ts, VegaPlayer.Source.SNITCH
        ));
        vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                trackedPlayers.get(uuid)
        ));

        queuedSnitchAlerts.put(uuid, snitchAlert);
        if (voxelmapEnabled) {
            // TODO only sync the waypoint we just got
            voxelmapIntegration.sync();
        }
    }

    @Override
    public boolean handlePacketSending(Packet<?> packet) {
        if (packet instanceof ServerboundPlayerLoadedPacket splp) {
            if (voxelmapEnabled) {
                voxelmapIntegration.clearManagedWaypoints();
                voxelmapIntegration.start();
            }
        }
        return false;
    }

    public void stopMap() {
        if (voxelmapEnabled) {
            voxelmapIntegration.stop();
        }
    }

    public void startMap() {
        if (voxelmapEnabled) {
            voxelmapIntegration.start();
        }
    }

    public static WebSocket getWebSocket() {
        return webSocket;
    }

    public void tryWSConnection() {
        String url = config.getWssURL();
        if (!url.endsWith("/")) {
            url += "/";
        }
        String connectionURL = MessageFormat.format(url + "?serverId={0}&username={1}", this.serverHash, Minecraft.getInstance().player.getName().getString());
        try {
            webSocket = HttpClient
                    .newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(connectionURL), new VegaWebsocketHandler(this))
                    .join();
        } catch (Exception e) {
            LOGGER.info("Unable to connect to WS server {}. Is it down?", config.getWssURL());
            LOGGER.error(e.getMessage(), e);
        }
    }

    public int getStatus(UUID uuid) {
        VegaUser optional = vegaUsers.get(uuid);
        return optional != null ? optional.status() : -1;
    }

    public int getPermissionLevel(UUID uuid) {
        VegaUser optional = vegaUsers.get(uuid);
        return optional != null ? optional.permission() : -1;
    }

    public void tick(Minecraft minecraft) {
        if (listKey.consumeClick()) {
            minecraft.setScreen(new VegaPlayerScreen());
        } else if (renderKey.consumeClick()) {
            config.setRenderEnabled(!config.isRenderEnabled());
            config.save();
            minecraft.player.displayClientMessage(Component.literal("Vega rendering " + (config.isRenderEnabled() ? "enabled" : "disabled")), true);
        } else if (settingsKey.consumeClick()) {
            minecraft.setScreen(new SettingsScreen(minecraft.screen));
        }
    }

    public void sendVegaUser(VegaUser vegaUser) {
        if (webSocket != null && config.isSendInfo()) {
            webSocket.sendText("?user=" + gson.toJson(vegaUser, VegaUser.class), true);
        }
    }
}
