package ca.favro.vega.common.gui.components;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.List;

public class IconToast implements Toast {
    private Identifier BACKGROUND_SPRITE = Identifier.fromNamespaceAndPath("vega", "toast_background");
    private final Identifier icon;
    private final Component title;
    private final List<FormattedCharSequence> text;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    private long displayTime;
    private final int width;

    public IconToast(Component title, Component text, Identifier resourceLocation, long displayTime) {
        this.icon = resourceLocation;
        this.title = title;
        this.text = Minecraft.getInstance().font.split(text, 200);
        this.displayTime = displayTime;
        this.width = Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(title), text == null ? 0 : Minecraft.getInstance().font.width(text)));
        Minecraft.getInstance().execute(() -> {
            NativeImage nativeImage;
            try {
                nativeImage = NativeImage.read(getClass().getResourceAsStream("/toast_background.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Minecraft.getInstance().getTextureManager().register(BACKGROUND_SPRITE, new DynamicTexture(() -> "vega:toast_background", nativeImage));
        });
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return 20 + Math.max(this.text.size(), 1) * 12;
    }

    @Override
    public Toast.@NonNull Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(@NonNull ToastManager toastManager, long l) {
        double d = this.displayTime * toastManager.getNotificationDisplayTimeMultiplier();
        this.wantedVisibility = l < d ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, @NonNull Font font, long l) {
        this.blitNineSlicedSprite(guiGraphics, RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE,
                new GuiSpriteScaling.NineSlice(160, 64, new GuiSpriteScaling.NineSlice.Border(17, 30, 4, 4), true),
                -12, 0, this.width() + 12, this.height(), -1);
        if (this.text.isEmpty()) {
            guiGraphics.drawString(font, this.title, 14, 12, -256, false);
        } else {
            guiGraphics.drawString(font, this.title, 14, 7, -256, false);

            for (int i = 0; i < this.text.size(); i++) {
                guiGraphics.drawString(font, this.text.get(i), 14, 18 + i * 12, -1, false);
            }
        }
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, -4, 8, 15, 15);
    }

    private void blitNineSlicedSprite(GuiGraphics guiGraphics, RenderPipeline pipeline, Identifier sprite, GuiSpriteScaling.NineSlice nineSlice, int x, int y, int width, int height, int color) {
        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
        int i = Math.min(border.left(), width / 2);
        int j = Math.min(border.right(), width / 2);
        int k = Math.min(border.top(), height / 2);
        int l = Math.min(border.bottom(), height / 2);
        if (width == nineSlice.width() && height == nineSlice.height()) {
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, height, color);
        } else if (height == nineSlice.height()) {
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, i, height, color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x + i, y, width - j - i, height, i, 0, nineSlice.width() - j - i, nineSlice.height(), nineSlice.width(), nineSlice.height(), color);
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, 0, x + width - j, y, j, height, color);
        } else if (width == nineSlice.width()) {
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, k, color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x, y + k, width, height - l - k, 0, k, nineSlice.width(), nineSlice.height() - l - k, nineSlice.width(), nineSlice.height(), color);
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - l, x, y + height - l, width, l, color);
        } else {
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, i, k, color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x + i, y, width - j - i, k, i, 0, nineSlice.width() - j - i, k, nineSlice.width(), nineSlice.height(), color);
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, 0, x + width - j, y, j, k, color);
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - l, x, y + height - l, i, l, color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x + i, y + height - l, width - j - i, l, i, nineSlice.height() - l, nineSlice.width() - j - i, l, nineSlice.width(), nineSlice.height(), color);
            guiGraphics.blit(pipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - j, nineSlice.height() - l, x + width - j, y + height - l, j, l, color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x, y + k, i, height - l - k, 0, k, i, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height(), color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x + i, y + k, width - j - i, height - l - k, i, k, nineSlice.width() - j - i, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height(), color);
            this.blitNineSliceInnerSegment(guiGraphics, nineSlice, sprite, x + width - j, y + k, j, height - l - k, nineSlice.width() - j, k, j, nineSlice.height() - l - k, nineSlice.width(), nineSlice.height(), color);
        }
    }

    private void blitNineSliceInnerSegment(GuiGraphics guiGraphics, GuiSpriteScaling.NineSlice nineSlice, Identifier sprite, int borderMinX, int borderMinY, int borderMaxX, int borderMaxY, int u, int v, int spriteWidth, int spriteHeight, int textureWidth, int textureHeight, int color) {
        if (borderMaxX > 0 && borderMaxY > 0) {
            if (nineSlice.stretchInner()) {
                guiGraphics.blit(sprite, borderMinX, borderMinY, borderMinX + borderMaxX, borderMinY + borderMaxY, this.getU((float) u / (float) textureWidth), this.getU((float) (u + spriteWidth) / (float) textureWidth), this.getV((float) v / (float) textureHeight), this.getV((float) (v + spriteHeight) / (float) textureHeight));
            }
        }
    }

    public float getU(float u) {
        float f = 0.4580078f - 0.3017578f;
        return 0.3017578f + f * u;
    }

    public float getV(float v) {
        float f = 0.06347656f - 9.765625E-4f;
        return 9.765625E-4f + f * v;
    }
}
