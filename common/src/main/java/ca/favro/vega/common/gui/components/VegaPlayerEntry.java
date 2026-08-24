package ca.favro.vega.common.gui.components;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Supplier;

import static ca.favro.vega.common.renderers.Utils.status2Color;

@Environment(EnvType.CLIENT)
public class VegaPlayerEntry extends ContainerObjectSelectionList.Entry<VegaPlayerEntry> {
    private static final Identifier DRAFT_REPORT_SPRITE = Identifier.withDefaultNamespace("icon/draft_report");
    private static final Duration TOOLTIP_DELAY = Duration.ofMillis(500L);
    private static final WidgetSprites REPORT_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("social_interactions/report_button"),
            Identifier.withDefaultNamespace("social_interactions/report_button_disabled"),
            Identifier.withDefaultNamespace("social_interactions/report_button_highlighted")
    );
    private static final WidgetSprites MUTE_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("social_interactions/mute_button"),
            Identifier.withDefaultNamespace("social_interactions/mute_button_highlighted")
    );
    private static final WidgetSprites UNMUTE_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("social_interactions/unmute_button"),
            Identifier.withDefaultNamespace("social_interactions/unmute_button_highlighted")
    );
    private final Minecraft minecraft;
    private final List<AbstractWidget> children;

    private final UUID id;
    private final String playerName;
    private final Supplier<PlayerSkin> skinGetter;
    private boolean isOnline;
    private Button allyButton;
    private Button enemyButton;
    private Button watchButton;
    private Button neutralButton;
    private Vega vega;
    private float tooltipHoverTime;
    private static final Component HIDDEN = Component.translatable("gui.socialInteractions.status_hidden").withStyle(ChatFormatting.ITALIC);
    private static final Component BLOCKED = Component.translatable("gui.socialInteractions.status_blocked").withStyle(ChatFormatting.ITALIC);
    private static final Component OFFLINE = Component.translatable("gui.socialInteractions.status_offline").withStyle(ChatFormatting.ITALIC);
    private static final Component HIDDEN_OFFLINE = Component.translatable("gui.socialInteractions.status_hidden_offline").withStyle(ChatFormatting.ITALIC);
    private static final Component BLOCKED_OFFLINE = Component.translatable("gui.socialInteractions.status_blocked_offline").withStyle(ChatFormatting.ITALIC);
    private static final int SKIN_SIZE = 24;
    private static final int PADDING = 4;
    public static final int SKIN_SHADE = ARGB.color(190, 0, 0, 0);
    public static final int BG_FILL = ARGB.color(255, 74, 74, 74);
    public static final int BG_FILL_REMOVED = ARGB.color(255, 48, 48, 48);
    public static final int PLAYER_STATUS_COLOR = ARGB.color(140, 255, 255, 255);

    public VegaPlayerEntry(Minecraft minecraft, UUID uUID, String string, Supplier<PlayerSkin> skinGetter, boolean isOnline) {
        this.minecraft = minecraft;
        this.id = uUID;
        this.playerName = string;
        this.skinGetter = skinGetter;
        this.isOnline = isOnline;
        this.vega = Vega.getInstance();
        this.neutralButton = new ImageButton(
                0,
                0,
                20,
                20,
                REPORT_BUTTON_SPRITES,
                button -> {
                    VegaUser optional = vega.getVegaUsers().get(uUID);
                    VegaUser newUser;
                    if (optional != null) {
                        newUser = optional.withStatus(0);
                        vega.getVegaUsers().put(uUID, newUser);
                    } else {
                        newUser = new VegaUser(playerName, uUID, 0, 0);
                        vega.getVegaUsers().put(uUID, newUser);
                    }

                    vega.sendVegaUser(newUser);
                },
                Component.literal(String.format("%s is already neutral", playerName))
        );
        this.neutralButton.setTooltip(Tooltip.create(Component.literal(String.format("Mark %s as neutral", playerName))));

        this.allyButton = new ImageButton(
                0,
                0,
                20,
                20,
                REPORT_BUTTON_SPRITES,
                button -> {
                    VegaUser optional = vega.getVegaUsers().get(uUID);
                    VegaUser newUser;
                    if (optional != null) {
                        newUser = optional.withStatus(1);
                        vega.getVegaUsers().put(uUID, newUser);
                    } else {
                        newUser = new VegaUser(playerName, uUID, 1, 0);
                        vega.getVegaUsers().put(uUID, newUser);
                    }

                    vega.sendVegaUser(newUser);
                },
                Component.literal(String.format("%s is already an ally", playerName))
        );
        this.allyButton.setTooltip(Tooltip.create(Component.literal(String.format("Mark %s as ally", playerName))));

        this.watchButton = new ImageButton(
                0,
                0,
                20,
                20,
                REPORT_BUTTON_SPRITES,
                button -> {
                    VegaUser optional = vega.getVegaUsers().get(uUID);
                    VegaUser newUser;
                    if (optional != null) {
                        newUser = optional.withStatus(2);
                        vega.getVegaUsers().put(uUID, newUser);
                    } else {
                        newUser = new VegaUser(playerName, uUID, 2, 0);
                        vega.getVegaUsers().put(uUID, newUser);
                    }

                    vega.sendVegaUser(newUser);
                },
                Component.literal(String.format("%s is already watched", playerName))
        );
        this.watchButton.setTooltip(Tooltip.create(Component.literal(String.format("Mark %s as watch", playerName))));


        this.enemyButton = new ImageButton(
                0,
                0,
                20,
                20,
                REPORT_BUTTON_SPRITES,
                button -> {
                    VegaUser optional = vega.getVegaUsers().get(uUID);
                    VegaUser newUser;
                    if (optional != null) {
                        newUser = optional.withStatus(3);
                        vega.getVegaUsers().put(uUID, newUser);
                    } else {
                        newUser = new VegaUser(playerName, uUID, 3, 0);
                        vega.getVegaUsers().put(uUID, newUser);
                    }

                    vega.sendVegaUser(newUser);
                },
                Component.literal(String.format("%s is already an enemy", playerName))
        );
        this.enemyButton.setTooltip(Tooltip.create(Component.literal(String.format("Mark %s as enemy", playerName))));


        this.children = new ArrayList<>();
        this.children.add(this.neutralButton);
        this.children.add(this.allyButton);
        this.children.add(this.watchButton);
        this.children.add(this.enemyButton);
    }

    @Override
    public int getContentY() {
        return this.getY() + 2 + 1;
    }

    @Override
    public int getContentX() {
        return this.getX() + 2 + 1;
    }

    @Override
    public void renderContent(GuiGraphics guiGraphics, int i, int j, boolean bl, float f) {
        int k = this.getContentX() + 4;
        int l = this.getContentY() + (this.getContentHeight() - 24) / 2;
        int m = k + 24 + 4;
        int n;

        n = this.getContentY() + (this.getContentHeight() - (9 + 9)) / 2;
        if (vega.getFocusedPlayers().contains(getPlayerId())) {
            guiGraphics.fill(this.getContentX() - 1, this.getContentY() - 1, this.getContentRight() + 1, this.getContentBottom() + 1, 0xFFFFFFFF);
        }

        boolean isFocused = vega.getFocusedPlayers().contains(getPlayerId());

        StringJoiner subString = new StringJoiner(" ");
        if (isFocused) subString.add("(Focused)");
        if (isOnline) {
            guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL);
            if (isFocused) {
                guiGraphics.drawString(this.minecraft.font, Component.literal(subString.toString()), m, n + 12, PLAYER_STATUS_COLOR);
            }
        } else {
            guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL_REMOVED);
            guiGraphics.drawString(this.minecraft.font, Component.literal(subString.add("(Offline)").toString()), m, n + 12, PLAYER_STATUS_COLOR);
        }

        PlayerFaceRenderer.draw(guiGraphics, this.skinGetter.get(), k, l, 24);
        MutableComponent name = Component.literal(this.playerName).withColor(status2Color(vega.getStatus(this.id)));
        VegaUser user = vega.getVegaUsers().get(getPlayerId());
        if (user != null && user.permission() >= 1) {
            Component a = Component.literal(" (Auth)").withColor(0xFFFFFFFF);
            name.append(a);
        }

        if (vega.getVegaUsers().containsKey(getPlayerId()) && vega.getVegaUsers().get(getPlayerId()).permission() == 2) {
            name = name.copy().withStyle(ChatFormatting.BOLD);
        }

        guiGraphics.drawString(this.minecraft.font, name, m, n, 0xFFFFFFFF);
        if (!this.isOnline) {
            guiGraphics.fill(k, l, k + 24, l + 24, SKIN_SHADE);
        }

        if (this.allyButton != null && this.enemyButton != null && this.watchButton != null && this.neutralButton != null) {
            float g = this.tooltipHoverTime;
            boolean canModify = vega.getPermissionLevel(this.minecraft.player.getUUID()) == 2;

            this.neutralButton.setX(this.getContentX() + (this.getContentWidth() - this.allyButton.getWidth() - 4) - 60);
            this.neutralButton.setY(this.getContentY() + (this.getContentHeight() - this.allyButton.getHeight()) / 2);
            this.neutralButton.active = canModify && !(user == null || user.status() == 0);


            this.allyButton.setX(this.getContentX() + (this.getContentWidth() - this.allyButton.getWidth() - 4) - 40);
            this.allyButton.setY(this.getContentY() + (this.getContentHeight() - this.allyButton.getHeight()) / 2);
            this.allyButton.active = canModify && (user == null || user.status() != 1);


            this.watchButton.setX(this.getContentX() + (this.getContentWidth() - this.watchButton.getWidth() - 4) - 20);
            this.watchButton.setY(this.getContentY() + (this.getContentHeight() - this.watchButton.getHeight()) / 2);
            this.watchButton.active = canModify && (user == null || user.status() != 2);


            this.enemyButton.setX(this.getContentX() + (this.getContentWidth() - this.enemyButton.getWidth() - 4));
            this.enemyButton.setY(this.getContentY() + (this.getContentHeight() - this.enemyButton.getHeight()) / 2);
            this.enemyButton.active = canModify && (user == null || user.status() != 3);

            if (canModify) {
                this.neutralButton.render(guiGraphics, i, j, f);
                this.allyButton.render(guiGraphics, i, j, f);
                this.watchButton.render(guiGraphics, i, j, f);
                this.enemyButton.render(guiGraphics, i, j, f);
            }

            if (g == this.tooltipHoverTime) {
                this.tooltipHoverTime = 0.0F;
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.children;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        boolean canModify = vega.getPermissionLevel(this.minecraft.player.getUUID()) == 2;
        if (mouseButtonEvent.hasShiftDown()) {
            if (vega.getFocusedPlayers().contains(getPlayerId())) {
                vega.getFocusedPlayers().remove(getPlayerId());
            } else {
                vega.getFocusedPlayers().add(getPlayerId());
            }

            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } else if ((mouseButtonEvent.hasControlDownWithQuirk() || mouseButtonEvent.hasControlDown()) && canModify) {
            VegaUser optional = vega.getVegaUsers().get(getPlayerId());
            VegaUser newUser;
            if (optional != null) {
                if (optional.permission() == 2) return super.mouseClicked(mouseButtonEvent, bl);

                if (optional.permission() == 1) {
                    newUser = optional.withPermission(0).withStatus(0);
                    vega.getVegaUsers().put(getPlayerId(), newUser);
                } else {
                    newUser = optional.withPermission(1).withStatus(1);
                    vega.getVegaUsers().put(getPlayerId(), newUser);
                }
            } else {
                newUser = new VegaUser(playerName, getPlayerId(), 1, 1);
                vega.getVegaUsers().put(getPlayerId(), newUser);
            }

            vega.sendVegaUser(newUser);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARROW_HIT_PLAYER, 0.25F));
        }

        return super.mouseClicked(mouseButtonEvent, bl);
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public UUID getPlayerId() {
        return this.id;
    }

    public void setOnline(boolean bl) {
        this.isOnline = bl;
    }

    private Component getStatusComponent() {
        boolean bl = this.minecraft.getPlayerSocialManager().isHidden(this.id);
        boolean bl2 = this.minecraft.getPlayerSocialManager().isBlocked(this.id);
        if (bl2 && this.isOnline) {
            return BLOCKED_OFFLINE;
        } else if (bl && this.isOnline) {
            return HIDDEN_OFFLINE;
        } else if (bl2) {
            return BLOCKED;
        } else if (bl) {
            return HIDDEN;
        } else {
            return this.isOnline ? OFFLINE : CommonComponents.EMPTY;
        }
    }
}
