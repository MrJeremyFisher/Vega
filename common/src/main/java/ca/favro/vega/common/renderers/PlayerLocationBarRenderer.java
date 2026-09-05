package ca.favro.vega.common.renderers;


import ca.favro.vega.common.Vega;
import ca.favro.vega.common.waypoint.VegaPlayerWaypoint;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.Waypoint;

import java.time.Instant;
import java.util.Objects;

import static ca.favro.vega.common.renderers.Utils.status2Color;

public class PlayerLocationBarRenderer implements ContextualBarRenderer {
    // For now we just reuse the Mojang locator textures because they're nice enough
    private static final Identifier LOCATOR_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private Vega vega;

    public PlayerLocationBarRenderer(Vega vega) {
        this.vega = vega;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_BACKGROUND, this.left(this.minecraft.getWindow()), this.minecraft.getWindow().getGuiScaledHeight() - 48 - 5, 182, 5
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int i = this.top(this.minecraft.getWindow());
        Entity entity = this.minecraft.getCameraEntity();
        if (entity != null) {
            vega.getVegaWaypointManager().forEachWaypointSorted(
                    (trackedWaypoint) -> {
                        if (!trackedWaypoint.id().equals(entity.getUUID()) // Don't render own waypoint
                                && (((Instant.now().toEpochMilli() - trackedWaypoint.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())
                        ) {
                            if (vega.config.isShowOnlyFocused()) {
                                if (vega.getFocusedPlayers().contains(trackedWaypoint.id())) {
                                    extracted(guiGraphics, trackedWaypoint, entity, i);
                                }
                            } else {
                                extracted(guiGraphics, trackedWaypoint, entity, i);
                            }
                        }
                    }, vegaPlayerWaypoint -> minecraft.level.dimension().identifier().getPath().equals(vegaPlayerWaypoint.getWorld())
                            && Objects.equals(vega.getCurrentServerString(), vegaPlayerWaypoint.getServer())
            );
        }
    }

    private void extracted(GuiGraphics guiGraphics, VegaPlayerWaypoint trackedWaypoint, Entity entity, int i) {
        double d = trackedWaypoint.yawAngleToCamera(this.minecraft.gameRenderer.getMainCamera());
        if (!(d <= -60.0) && !(d > 60.0)) {
            int j = Mth.ceil((guiGraphics.guiWidth() - 9) / 2.0F);
            Waypoint.Icon icon = trackedWaypoint.icon();
            WaypointStyle waypointStyle = this.minecraft.getWaypointStyles().get(icon.style);
            float f = Mth.sqrt((float) trackedWaypoint.distanceSquared(entity));
            Identifier identifier = waypointStyle.sprite(f);
            int k = status2Color(Vega.getInstance().getStatus(trackedWaypoint.id()));
            int l = Mth.floor(d * 173.0 / 2.0 / 60.0);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, j + l, i - 2, 9, 9, k);
            if ((d <= 5.0 && d >= -5.0) && minecraft.options.keyShift.isDown()) {
                Font font = minecraft.font;
                String name = trackedWaypoint.getName();
                // Have to manually fill bgcolour as it doesn't do anything in GuiTextRenderState
                int halfWidth = font.width(name) / 2;
                guiGraphics.fill(j + l - 2 - halfWidth + (9 / 2), i + 10 + 1 - 10, j + l + 2 + halfWidth + (9 / 2), i - 10 - 1, 0x7F000001);
                guiGraphics.guiRenderState.submitText(new GuiTextRenderState(font,
                        Language.getInstance().getVisualOrder(FormattedText.of(name)),
                        guiGraphics.pose(), j + (9 / 2) + l - halfWidth, i - 10, k, 0,
                        false, false, guiGraphics.scissorStack.peek()));
            }
        }
    }
}
