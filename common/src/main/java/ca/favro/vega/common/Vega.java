package ca.favro.vega.common;

import ca.favro.vega.client.IVega;
import ca.favro.vega.common.config.VegaConfig;
import ca.favro.vega.common.gui.components.IconToast;
import ca.favro.vega.common.gui.screens.SettingsScreen;
import ca.favro.vega.common.gui.screens.VegaPlayerScreen;
import ca.favro.vega.common.integrations.civmodern.CivModernIntegration;
import ca.favro.vega.common.integrations.combatradar.CombatRadarIntegration;
import ca.favro.vega.common.integrations.journeymap.JourneymapIntegration;
import ca.favro.vega.common.integrations.voxelmap.VoxelmapIntegration;
import ca.favro.vega.common.integrations.xaero.XaeroMinimapIntegration;
import ca.favro.vega.common.mixin.mixins.TablistAccessorMixin;
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
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashSet;
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
    private static final String MOD_VERSION = "1.0.1-1.21.11";
    public final Logger LOGGER;
    private static Vega instance = null;
    private String serverHash;
    private static WebSocket webSocket;
    private final Set<UUID> focusedPlayers = new HashSet<>();
    private final Map<UUID, VegaPlayer> trackedPlayers = new ConcurrentHashMap<>();
    private Map<UUID, VegaUser> vegaUsers = new ConcurrentHashMap<>();
    private final Map<UUID, SnitchAlert> queuedSnitchAlerts = new ConcurrentHashMap<>();
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
    private final KeyMapping settingsKey;
    private final KeyMapping renderKey;
    private final KeyMapping listKey;
    private final boolean combatRadarEnabled;
    private final boolean voxelmapEnabled;
    private VoxelmapIntegration voxelmapIntegration;
    private final boolean journeymapEnabled;
    private JourneymapIntegration journeymapIntegration;
    private final boolean civmodernEnabled;
    private CivModernIntegration civModernIntegration;
    private final IconToast connectedToast = new IconToast(
            Component.literal("Connected"),
            Component.literal("Connected to Vega server"),
            Identifier.withDefaultNamespace("icon/link"),
            2000L
    );
    private final IconToast disconnectedToast = new IconToast(
            Component.literal("Disconnected"),
            Component.literal("Disconnected from Vega server"),
            Identifier.withDefaultNamespace("player_list/remove_player"),
            10000L
    );

    private final boolean xaerosmapEnabled;
    private XaeroMinimapIntegration xaeroMinimapIntegration;
    private static final Pattern oldWorldKey = Pattern.compile("ResourceKey\\[minecraft:dimension \\/ minecraft:(.*)\\]");

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
        this.xaerosmapEnabled = FabricLoader.getInstance().isModLoaded("xaerominimap");
        this.journeymapEnabled = FabricLoader.getInstance().isModLoaded("journeymap");
        this.civmodernEnabled = FabricLoader.getInstance().isModLoaded("civmodern");
        if (voxelmapEnabled) {
            voxelmapIntegration = new VoxelmapIntegration(this, Minecraft.getInstance());
        }
        if (xaerosmapEnabled) {
            xaeroMinimapIntegration = new XaeroMinimapIntegration(this, Minecraft.getInstance());
        }
        if (journeymapEnabled) {
            journeymapIntegration = new JourneymapIntegration(this, Minecraft.getInstance());
        }
        if (civmodernEnabled) {
            civModernIntegration = new CivModernIntegration(this, Minecraft.getInstance());
        }
    }

    public static void popOpenToast() {
        // TODO remove disconnected toast
        Minecraft instance = Minecraft.getInstance();
        instance.execute(() -> {
            if (instance.player != null) {
                instance.player.displayClientMessage(Component.literal("[Vega] Connected to Vega server"), false);
            }
        });
        // TODO reenable when I figure out nineslice
//        Minecraft.getInstance().getToastManager().addToast(
//                getInstance().connectedToast
//        );
    }

    public static void popCloseToast() {
        Minecraft instance = Minecraft.getInstance();
        instance.execute(() -> {
            if (instance.player != null) {
                instance.player.displayClientMessage(Component.literal("[Vega] Disconnected from Vega server"), false);
            }
        });

//        Minecraft.getInstance().getToastManager().addToast(
//                getInstance().disconnectedToast
//        );
    }

    public Map<UUID, VegaUser> getVegaUsers() {
        return vegaUsers;
    }

    public void setVegaUsers(Map<UUID, VegaUser> vegaUsers) {
        this.vegaUsers = vegaUsers;
        if (this.combatRadarEnabled) {
            CombatRadarIntegration.syncStatuses();
        }
        syncMaps();
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
                LOGGER.error(e.getMessage(), e);
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
                // to other users. Times in the hits will handle syncing them on other clients
                for (UUID snitchAlertUUID : queuedSnitchAlerts.keySet()) {
                    SnitchAlert sa = queuedSnitchAlerts.get(snitchAlertUUID);
                    if (webSocket != null && config.isSendInfo()) {
                        // TODO can you get hits from Zorweth on main? If so, filter on that
                        webSocket.sendText("?player=" + gson.toJson(new VegaPlayer(
                                sa.accountName, snitchAlertUUID, sa.pos, sa.world, getCurrentServerString(), sa.ts, VegaPlayer.Source.SNITCH
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
        // Only try if not already connected
        if (webSocket == null || webSocket.isOutputClosed()) {
            String addr = clientPacketListener.getConnection().getRemoteAddress().toString();
            if (addr.contains("23.163.152.211") || addr.contains("play.civmc.net")) {
                tryWSConnection();
                playerSenderRunner = Executors.newScheduledThreadPool(1);
                playerSenderRunner.scheduleAtFixedRate(playerSender, 0, 3, TimeUnit.SECONDS);
            } else {
                handleDisconnectedFromServer(null, null);
            }
        }
    }

    @Override
    public void handleDisconnectedFromServer(DisconnectionDetails disconnectionDetails, Connection self) {
        if (playerSenderRunner != null) {
            playerSenderRunner.close();
        }

        playerSender.run();
        trackedPlayers.clear();
        vegaUsers.clear();
        vegaWaypointManager.untrackAllWaypoints();

        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client shutting down");
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
//        Optional<Map.Entry<UUID, VegaPlayer>> duplicate = trackedPlayers.entrySet().stream().filter(
//                entry ->
//                        entry.getValue().name().equals(player.getName().getString())
//                                && !entry.getKey().equals(player.getUUID())
//        ).findFirst();
//        // Clear combat logger waypoints
//        if (duplicate.isPresent()) {
//            Map.Entry<UUID, VegaPlayer> duplicateF = duplicate.get();
//            trackedPlayers.remove(duplicateF.getKey());
//            vegaWaypointManager.untrackWaypoint(duplicateF.getKey());
//        }
        trackedPlayers.put(player.getUUID(),
                new VegaPlayer(player.getName().getString(),
                        player.getUUID(),
                        pos,
                        Minecraft.getInstance().level.dimension().identifier().getPath(), getCurrentServerString(),
                        System.currentTimeMillis(), VegaPlayer.Source.LOCAL)
        );
        vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                trackedPlayers.get(player.getUUID())
        ));
    }

    public void handleScreenshot(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        // TODO: To censor info, disable all Vega rendering (incl. map wps), render one frame, save that frame as the screenshot,
        // TODO: cancel the original screenshot
    }

    public void receiveRemotePlayer(VegaPlayer receivedPlayer) {
        Matcher matcher = oldWorldKey.matcher(receivedPlayer.world());
        if (matcher.matches()) {
            receivedPlayer = new VegaPlayer(receivedPlayer.name(),
                    receivedPlayer.uuid(),
                    receivedPlayer.position(),
                    matcher.group(1), getCurrentServerString(), receivedPlayer.time(), receivedPlayer.source());
        }

        if (this.trackedPlayers.containsKey(receivedPlayer.uuid())) {
            if (this.trackedPlayers.get(receivedPlayer.uuid()).time() < receivedPlayer.time()) {
                trackedPlayers.put(receivedPlayer.uuid(),
                        receivedPlayer
                );
                vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                        trackedPlayers.get(receivedPlayer.uuid())
                ));
                syncMaps();
            }
        } else {
            trackedPlayers.put(receivedPlayer.uuid(),
                    receivedPlayer
            );
            vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                    trackedPlayers.get(receivedPlayer.uuid())
            ));
            syncMaps();
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
            if (civmodernEnabled) return false;
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
                    boolean isOld = (Instant.now().toEpochMilli() - p.time() / 1000) > 5;
                    if (p.source() != VegaPlayer.Source.LOCAL || isOld) {
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
                snitchAlert.accountName, uuid, snitchAlert.pos, snitchAlert.world, getCurrentServerString(), snitchAlert.ts, VegaPlayer.Source.SNITCH
        ));
        vegaWaypointManager.trackOrUpdate(new VegaPlayerWaypoint(
                trackedPlayers.get(uuid)
        ));

        queuedSnitchAlerts.put(uuid, snitchAlert);
        // TODO only sync the waypoint we just got
        syncMaps();
    }

    @Override
    public boolean handlePacketSending(Packet<?> packet) {
        if (packet instanceof ServerboundPlayerLoadedPacket splp) {
            if (this.voxelmapEnabled) {
                voxelmapIntegration.start();
            }
            if (this.xaerosmapEnabled) {
                xaeroMinimapIntegration.clearManagedWaypoints();
                xaeroMinimapIntegration.start();
            }
            if (this.journeymapEnabled) {
                journeymapIntegration.clearManagedWaypoints();
                journeymapIntegration.start();
            }
            if (this.civmodernEnabled) {
                civModernIntegration.clearManagedWaypoints();
                civModernIntegration.start();
            }
        }
        return false;
    }

    public void stopMap() {
        if (this.voxelmapEnabled) {
            voxelmapIntegration.stop();
        }
        if (this.xaerosmapEnabled) {
            xaeroMinimapIntegration.stop();
        }
        if (this.journeymapEnabled) {
            journeymapIntegration.stop();
        }
        if (this.civmodernEnabled) {
            civModernIntegration.stop();
        }
    }

    public void startMap() {
        if (this.voxelmapEnabled) {
            voxelmapIntegration.start();
        }
        if (this.xaerosmapEnabled) {
            xaeroMinimapIntegration.start();
        }
        if (this.journeymapEnabled) {
            journeymapIntegration.start();
        }
        if (this.civmodernEnabled) {
            civModernIntegration.start();
        }
    }

    public void syncMaps() {
        if (this.voxelmapEnabled) {
            voxelmapIntegration.sync();
        }
        if (this.xaerosmapEnabled) {
            xaeroMinimapIntegration.sync();
        }
        if (this.journeymapEnabled) {
            journeymapIntegration.sync();
        }
        if (this.civmodernEnabled) {
            civModernIntegration.sync();
        }
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public Map<UUID, VegaPlayer> getTrackedPlayers() {
        return trackedPlayers;
    }

    public VoxelmapIntegration getVoxelmapIntegration() {
        return voxelmapIntegration;
    }

    public JourneymapIntegration getJourneymapIntegration() {
        return journeymapIntegration;
    }

    public void tryWSConnection() {
        String url = config.getWssURL();
        if (!url.endsWith("/")) {
            url += "/";
        }
        File tokenFile = new File(FabricLoaderImpl.INSTANCE.getGameDir().toFile(), "/vega/token");
        FileInputStream fileInputStream;
        String token = "";
        if (tokenFile.exists()) {
            try {
                fileInputStream = new FileInputStream(tokenFile);
                token = new String(fileInputStream.readAllBytes());
                fileInputStream.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
        String connectionURL;

        if (token.isBlank()) {
            connectionURL = MessageFormat.format("{0}?serverId={1}&username={2}", url, this.serverHash, Minecraft.getInstance().player.getName().getString());
        } else {
            connectionURL = MessageFormat.format("{0}?serverId={1}&username={2}&token={3}", url, this.serverHash, Minecraft.getInstance().player.getName().getString(), token.stripTrailing());
        }

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

    public String getCurrentServerString() {
//        Pattern tabPattern = Pattern.compile("/.* Welcome to CivMC *(.*)! .*/gi");
        Component tabHeader = ((TablistAccessorMixin) Minecraft.getInstance().gui.getTabList()).getHeader();
        if (tabHeader != null) {
//            String toMatch = tabHeader.getString().split("\n")[0];
//            Matcher matcher = tabPattern.matcher(toMatch.strip());
//            LOGGER.info(toMatch.strip());
//            boolean b = matcher.matches();
//            LOGGER.info("Matched " + b);
//            if (b) {
//                LOGGER.info("matched str: " + matcher.group(1) + "....");
//                return matcher.group(1);
//            }

            // TODO Auuuuuggghhhhh
            switch (tabHeader.getString().split("\n")[0]) {
                case "§6§kAAAA§r§6§l Welcome to CivMC! §r§6§kAAAA":
                    return "main";
                case "§6§kAAAA§r§6§l Welcome to CivMC §4§lPvP§6§l! §r§6§kAAAA":
                    return "pvp";
            }
        }
        return "UNKNOWN";
    }
}
