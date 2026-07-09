package ca.favro.vega.common.renderers;


import ca.favro.vega.common.Vega;
import ca.favro.vega.common.waypoint.VegaPlayerWaypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;

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
        }, minecraft.level.dimension().identifier().getPath());
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        buffers.endBatch();
    }

    private void renderSign(PoseStack poseStack, MultiBufferSource bufferSource, VegaPlayerWaypoint wp) {
        VegaPlayerWaypoint waypoint = new VegaPlayerWaypoint(wp);
        String mainLabel = waypoint.getName();

        float distance = (float) minecraft.getCameraEntity().position().distanceTo(waypoint.position());
        double maxDistance = minecraft.gameRenderer.getDepthFar() - 8.0;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            waypoint.setPosition(waypoint.position().multiply(1 / distance * maxDistance, 1 / distance * maxDistance, 1 / distance * maxDistance));
            adjustedDistance = maxDistance;
        }

        float scale = ((float) adjustedDistance * 0.15F + 1.0F) * 0.0266F;

        poseStack.pushPose();

        Vec3 pos = waypoint.position();
        Entity e = minecraft.getCameraEntity();
        pos = new Vec3(pos.x, e == null ? 64 : e.getY(), pos.z);
        poseStack.translate(pos
                .subtract(minecraft.getCameraEntity()
                        .getPosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)))
                .add(0f, 1.5f, 0f)
        );
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        poseStack.scale(scale, -scale, scale);

        float alpha = distance > 5.0 ? 1.0F : distance / 5.0F;

        Vec3 lookVector = minecraft.getCameraEntity().getViewVector(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)).normalize();
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
        int height = minecraft.level.getHeight();

        float spentTime = minecraft.getCameraEntity().tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float texturePos = Mth.frac(spentTime * 0.2F - Mth.floor(spentTime * 0.1F));

        poseStack.pushPose();
        poseStack.translate(waypoint.position().subtract(minecraft.getCameraEntity().getPosition(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false))));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spentTime * 2.25F - 45.0F));

        float beamRadius = BeaconRenderer.SOLID_BEAM_RADIUS / 1.4142F;
        float beamMaxV = 1.0F - texturePos;
        float beamMinV = height * (0.5F / BeaconRenderer.SOLID_BEAM_RADIUS) + beamMaxV;
        int beamColor = status2Color(vega.getStatus(waypoint.id()));
        RenderType beamRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, false);
        drawBeam(poseStack, bufferSource, height, beamRadius, beamMaxV, beamMinV, beamColor, beamRenderType);
        poseStack.popPose();

        float glowRadius = BeaconRenderer.BEAM_GLOW_RADIUS;
        float glowMaxV = 1.0F - texturePos;
        float glowMinV = height + beamMaxV;
        int glowColor = (status2Color(vega.getStatus(waypoint.id())) & 0x00FFFFFF) | 0x40000000;

        RenderType glowRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);
        drawBeam(poseStack, bufferSource, height, glowRadius, glowMaxV, glowMinV, glowColor, glowRenderType);
        poseStack.popPose();
    }

    private void drawBeam(PoseStack poseStack, MultiBufferSource bufferSource, int height, float glowRadius, float glowMaxV, float glowMinV, int glowColor, RenderType glowRenderType) {
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -glowRadius : glowRadius;
            float z = (face < 2) ? -glowRadius : glowRadius;
            float x2 = (face < 2) ? -glowRadius : glowRadius;
            float z2 = (face == 1 || face == 2) ? -glowRadius : glowRadius;

            glowBuffer.addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
        }
    }
}
