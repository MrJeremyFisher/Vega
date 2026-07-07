package ca.favro.vega.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

public class VegaFabric implements ClientModInitializer {
    public static final String MOD_ID = "vega";
    public static final String MOD_LOADER_VERSION = "1.0.1-1.21.11-ALPHA";
    public static final Logger LOGGER = new VegaLogger(VegaFabric.class);
    private final KeyMapping.Category keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vega", "keycategory"));
    private final KeyMapping renderKey = new KeyMapping("Toggle Vega Renderers", GLFW.GLFW_KEY_PERIOD, keyCategory);
    private final KeyMapping listKey = new KeyMapping("Vega Player List", GLFW.GLFW_KEY_SEMICOLON, keyCategory);
    private final KeyMapping settingsKey = new KeyMapping("Vega Settings", GLFW.GLFW_KEY_COMMA, keyCategory);
    private static VegaFabric INSTANCE;
    public static @Nullable ca.favro.vega.client.IVega vega;

    public VegaFabric() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Constructor called twice");
        } else {
            INSTANCE = this;
        }
    }

    public static VegaFabric getInstance() {
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Not initialized");
        }

        LOGGER.info("Vega loader version {}", MOD_LOADER_VERSION);
        KeyBindingHelper.registerKeyBinding(settingsKey);
        KeyBindingHelper.registerKeyBinding(renderKey);
        KeyBindingHelper.registerKeyBinding(listKey);
        ClientLifecycleEvents.CLIENT_STARTED.register(e -> init());
    }

    private void init() {
        try {
            File gameDirectory = Minecraft.getInstance().gameDirectory;
            File jarDir = new File(gameDirectory, "/vega/");
            if (!jarDir.isDirectory())
                jarDir.mkdir();

            BufferedInputStream bufferedInputStream = null;
            try {
                URL url = URI.create("https://wss.ve3jfo.ca/jars/common.jar").toURL();
                LOGGER.info("Downloading Vega client JAR");
                URLConnection urlConnection = url.openConnection();
                bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

                File jarFile = new File(jarDir.getPath() + "/vega-client.jar");
                if (jarFile.exists()) {
                    jarFile.delete();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
                bufferedInputStream.transferTo(fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();

                // TODO REMOVE IN PROD
                jarFile = new File("/home/jeremy/Documents/Vega api-impl split/Vega/common/build/libs/common.jar");
                // TODO

                FabricLauncherBase.getLauncher().addToClassPath(Paths.get(jarFile.getAbsolutePath()));
                LOGGER.info("Vega client JAR on classpath");
            } finally {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            }

            vega = (IVega) this.getClass().getClassLoader().loadClass("ca.favro.vega.common.Vega").getDeclaredConstructor(KeyMapping.class, KeyMapping.class, KeyMapping.class, Logger.class).newInstance(renderKey, listKey, settingsKey, LOGGER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientPlayConnectionEvents.JOIN.register((e, e1, e2) -> vega.handleConnectToServer(e));
        ClientPlayConnectionEvents.DISCONNECT.register((e, e1) -> vega.handleDisconnectedFromServer(null, null));
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath("vega", "overlays"),
                vega::renderOverlays
        );
        WorldRenderEvents.END_MAIN.register(((context) -> vega.renderWaypointBeams(context.matrices(), context.consumers())));
        ClientTickEvents.START_CLIENT_TICK.register(vega::tick);

        vega.init();
    }
}
