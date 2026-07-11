package ca.favro.vega.common.gui.screens;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.config.VegaConfig;
import ca.favro.vega.common.gui.components.SliderButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private EditBox wssEditBox;
    private Button toggleSendButton;
    private Button toggleReceiveButton;
    private Button showOnlyFocusedButton;
    private Button renderBeamsButton;
    private Button renderNamePlatesButton;
    private Button renderLocatorBarButton;
    private Button showOnMapButton;
    private SliderButton waypointAgeSlider;
    private Vega vega;
    private VegaConfig config;

    private int connectY;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Vega Settings"));
        this.parent = parent;
        this.vega = Vega.getInstance();
        this.config = vega.config;
    }

    @Override
    public void init() {
        int y = this.height / 3 - 16;
        int x = this.width / 2 - 100;

        wssEditBox = new EditBox(this.font, x, y, 200, 20, CommonComponents.EMPTY);
        wssEditBox.setHint(Component.literal("Server Address"));
        wssEditBox.setValue(config.getWssURL());
        addRenderableWidget(wssEditBox);

        connectY = (y += 24);
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Connect"), (btn) -> {
            vega.tryWSConnection();
        }).bounds(x, y, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Disconnect"), (btn) -> {
            if (vega.getWebSocket() != null) {
                vega.getWebSocket().sendClose(400, "User disconnect");
            }
        }).bounds(x + 101, y, 100, 20).build());

        y += 24;

        toggleSendButton = Button.builder(Component.literal("Sending: "), (btn) -> {
            config.setSendInfo(!config.isSendInfo());
            config.save();
        }).bounds(x, y, 100, 20).build();
        addRenderableWidget(toggleSendButton);

        toggleReceiveButton = Button.builder(Component.literal("Receiving: "), (btn) -> {
            config.setReceiveInfo(!config.isReceiveInfo());
            config.save();
        }).bounds(x + 101, y, 100, 20).build();
        addRenderableWidget(toggleReceiveButton);

        y += 24;

        showOnlyFocusedButton = Button.builder(Component.literal("Show only focused: "), (btn) -> {
            config.setShowOnlyFocused(!config.isShowOnlyFocused());
            config.save();
        }).bounds(x, y, 200, 20).build();
        addRenderableWidget(showOnlyFocusedButton);

        y += 24;

        renderBeamsButton = Button.builder(Component.literal("Render beams: "), (btn) -> {
            config.setShowBeams(!config.isShowBeams());
            config.save();
        }).bounds(x, y, 100, 20).build();
        addRenderableWidget(renderBeamsButton);

        renderNamePlatesButton = Button.builder(Component.literal("Render names: "), (btn) -> {
            config.setShowNamePlates(!config.isShowNamePlates());
            config.save();
        }).bounds(x + 101, y, 100, 20).build();
        addRenderableWidget(renderNamePlatesButton);

        y += 24;

        renderLocatorBarButton = Button.builder(Component.literal("Render locator bar: "), (btn) -> {
            config.setShowLocatorBar(!config.isShowLocatorBar());
            config.save();
        }).bounds(x, y, 200, 20).build();
        addRenderableWidget(renderLocatorBarButton);

        y += 24;

        showOnMapButton = Button.builder(Component.literal("Show waypoints on map: "), (btn) -> {
            if (config.isShowOnMap()) {
                Vega.getInstance().stopMap();
            } else {
                Vega.getInstance().startMap();
            }
            config.setShowOnMap(!config.isShowOnMap());
            config.save();
        }).bounds(x, y, 200, 20).build();
        addRenderableWidget(showOnMapButton);

        y += 24;

        waypointAgeSlider = new SliderButton(x, y, 200, 8F, 0.1F, "Waypoint keep age (hrs)", config.getWaypointKeepAge(), false);
        addRenderableWidget(waypointAgeSlider);
        y += 24;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> this.minecraft.setScreen(parent)).bounds(x, y, 200, 20).build());
    }

    @Override
    public void tick() {
        toggleSendButton.setMessage(Component.literal("Sending: " + (config.isSendInfo() ? "On" : "Off")));
        toggleReceiveButton.setMessage(Component.literal("Receiving: " + (config.isReceiveInfo() ? "On" : "Off")));
        showOnlyFocusedButton.setMessage(Component.literal("Show only focused: " + (config.isShowOnlyFocused() ? "On" : "Off")));
        renderBeamsButton.setMessage(Component.literal("Render beams: " + (config.isShowBeams() ? "On" : "Off")));
        renderNamePlatesButton.setMessage(Component.literal("Render names: " + (config.isShowNamePlates() ? "On" : "Off")));
        renderLocatorBarButton.setMessage(Component.literal("Render locator bar: " + (config.isShowLocatorBar() ? "On" : "Off")));
        showOnMapButton.setMessage(Component.literal("Show waypoints on map: " + (config.isShowOnMap() ? "On" : "Off")));

        boolean isChanged = false;

        isChanged = config.setWssURL(wssEditBox.getValue()) || isChanged;
        isChanged = config.setWaypointKeepAge(waypointAgeSlider.getValue()) || isChanged;

        if (isChanged) config.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 40, Color.WHITE.getRGB());
        guiGraphics.drawString(this.font, "Websocket address:", this.width / 2 - 100 - 100, connectY - 24 + 6, Color.WHITE.getRGB());

        Component connection;
        if (vega.getWebSocket() != null && !vega.getWebSocket().isInputClosed()) {
            connection = Component.literal("Connected").withColor(Color.GREEN.getRGB());
        } else {
            connection = Component.literal("Disconnected").withColor(Color.RED.getRGB());
        }

        guiGraphics.drawCenteredString(this.font, connection, this.width / 2, connectY, Color.LIGHT_GRAY.getRGB());

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}
