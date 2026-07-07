package ca.favro.vega.common.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class IconToast implements Toast {
    // TODO I CANT GET THESE TO WORK WITH CUSTOM TEXTURES!!!
    private final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private final Identifier icon;
    private final Component title;
    private final List<FormattedCharSequence> text;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    private long displayTime;

    public IconToast(Component title, Component text, Identifier resourceLocation, long displayTime) {
        this.icon = resourceLocation;
        this.title = title;
        this.text = Minecraft.getInstance().font.split(text, 200);
        this.displayTime = displayTime;
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
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, -12, 0, this.width() + 12, this.height());
        if (this.text.isEmpty()) {
            guiGraphics.drawString(font, this.title, 14, 12, -256, false);
        } else {
            guiGraphics.drawString(font, this.title, 14, 7, -256, false);

            for (int i = 0; i < this.text.size(); i++) {
                guiGraphics.drawString(font, this.text.get(i), 18, 18 + i * 12, -1, false);
            }
        }
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, -4, 8, 15, 15);
    }
}
