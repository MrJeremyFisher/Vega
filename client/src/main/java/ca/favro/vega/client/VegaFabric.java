package ca.favro.vega.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public class VegaFabric implements ClientModInitializer {
    public static final String MOD_ID = "vega";
    public static final String MOD_LOADER_VERSION = "1.0.0-1.21.11";
    public static final Logger LOGGER = VegaFabricPL.LOGGER;
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
            vega = (IVega) this.getClass().getClassLoader().loadClass("ca.favro.vega.common.Vega")
                    .getDeclaredConstructor(KeyMapping.class, KeyMapping.class, KeyMapping.class, Logger.class)
                    .newInstance(renderKey, listKey, settingsKey, LOGGER);
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
