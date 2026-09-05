package ca.favro.vega.common.renderers;


import ca.favro.vega.common.Vega;
import ca.favro.vega.common.waypoint.VegaPlayerWaypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.Objects;

import static ca.favro.vega.common.renderers.Utils.status2Color;

public class PlayerLocationBeamRenderer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private Vega vega;

    public PlayerLocationBeamRenderer(Vega vega) {
        this.vega = vega;
    }

    public void renderWaypointBeams(PoseStack poseStack, MultiBufferSource multiBufferSource) {
        Entity entity = this.minecraft.getCameraEntity();
        vega.getVegaWaypointManager().forEachWaypoint((vpw) -> {
            if (!vpw.id().equals(entity.getUUID())
                    && (((Instant.now().toEpochMilli() - vpw.getDateAdded()) / (3.6 * Math.pow(10, 6))) < vega.config.getWaypointKeepAge())
            ) {
                if (vega.config.isShowOnlyFocused()) {
                    if (vega.getFocusedPlayers().contains(vpw.id())) {
                        if (vega.config.isShowBeams()) renderBeam(poseStack, multiBufferSource, vpw);
                        if (vega.config.isShowNamePlates()) renderSign(poseStack, multiBufferSource, vpw);
                    }
                } else {
                    if (vega.config.isShowBeams()) renderBeam(poseStack, multiBufferSource, vpw);
                    if (vega.config.isShowNamePlates()) renderSign(poseStack, multiBufferSource, vpw);
                }
            }
        }, vegaPlayerWaypoint -> minecraft.level.dimension().identifier().getPath().equals(vegaPlayerWaypoint.getWorld())
                && Objects.equals(vega.getCurrentServerString(), vegaPlayerWaypoint.getServer()));
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        buffers.endBatch();
    }

    private void renderSign(PoseStack poseStack, MultiBufferSource bufferSource, VegaPlayerWaypoint wp) {
        VegaPlayerWaypoint waypoint = new VegaPlayerWaypoint(wp);
        String mainLabel = waypoint.getName();

        Vec3 cPos = minecraft.getEntityRenderDispatcher().camera.position();
        double x = waypoint.position().x - cPos.x;
        double y = waypoint.position().y - cPos.y;
        double z = waypoint.position().z - cPos.z;
        float distance = (float) Mth.length(x, y, z);
        // TODO make setting
        if (distance <= 3) {
            return;
        }

        poseStack.pushPose();
        float maxDistance = (Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4) - 2;
        float adjustedDistance = distance;
        if (distance > maxDistance) {
            x = x / distance * maxDistance;
            y = y / distance * maxDistance;
            z = z / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float scale = (adjustedDistance * 0.12F + 1.0F) * 0.0266F;
        poseStack.translate(x, y, z);
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.scale(scale, -scale, scale);

        float alpha = distance > 5.0 ? 1.0F : distance / 5.0F;

        Vec3 lookVector = minecraft.getCameraEntity().getViewVector(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)).normalize();
        Vec3 vectorBetween = waypoint.position().subtract(minecraft.getCameraEntity().position());
        boolean isLookingAt = lookVector.dot(vectorBetween.normalize()) > 1 - 1.0 / vectorBetween.length();
        if (!isLookingAt && !vega.getFocusedPlayers().contains(waypoint.id())) {
            alpha *= 0.25f;
        }

        String subLabel;
        if (distance > 1000.0) {
            double converted = distance / 1000.0;
            subLabel = (int) converted + "." + (int) ((converted - (int) converted) * 10) + "km";
        } else {
            subLabel = (int) distance + "." + (int) ((distance - (int) distance) * 10) + "m";
        }

        subLabel += Utils.getWaypointTimeString(waypoint);

        boolean renderMainLabel = !mainLabel.isEmpty();

        int halfWidthMainLabel = minecraft.font.width(mainLabel) / 2;
        int yPosMainLabel = 10;

        float subLabelScale = 0.75F;
        int halfWidthSubLabel = minecraft.font.width(subLabel) / 2;
        int yPosSubLabel = 26;

        // Render labels
        int textColor = (int) (255.0F * alpha) << 24 | (status2Color(vega.getStatus(waypoint.id())) & 0x00FFFFFF);

        float bgOpacity = minecraft.options.getBackgroundOpacity(0.25f);
        int bgColor = (int) (bgOpacity * 255.0f) << 24;
        if (renderMainLabel) {
            minecraft.font.drawInBatch(mainLabel, -halfWidthMainLabel, yPosMainLabel, textColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, bgColor, LightTexture.FULL_BRIGHT);
        }

        poseStack.pushPose();
        poseStack.scale(subLabelScale, subLabelScale, 1.0F);
        minecraft.font.drawInBatch(subLabel, -halfWidthSubLabel, yPosSubLabel, textColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, bgColor, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, VegaPlayerWaypoint wp) {
        VegaPlayerWaypoint waypoint = new VegaPlayerWaypoint(wp);
        Vec3 cPos = minecraft.getEntityRenderDispatcher().camera.position();
        double x = waypoint.position().x - cPos.x - 0.5;
        double y = waypoint.position().y - cPos.y;
        double z = waypoint.position().z - cPos.z - 0.5;
        float distance = (float) Mth.length(x, y, z);
        // TODO make setting
        if (distance <= 3 || distance > (Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4) - 2) {
            return;
        }
        float spentTime = minecraft.getCameraEntity().tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        int beamColor = (status2Color(vega.getStatus(waypoint.id())) & 0x00FFFFFF) | 0x80000000;
        BeaconRenderer.submitBeaconBeam(
                poseStack,
                minecraft.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage(),
                BeaconRenderer.BEAM_LOCATION,
                1.0F,
                spentTime,
                0,
                minecraft.level.getHeight(),
                beamColor,
                BeaconRenderer.SOLID_BEAM_RADIUS / 1.4142F,
                BeaconRenderer.BEAM_GLOW_RADIUS
        );
        poseStack.popPose();
    }
}
