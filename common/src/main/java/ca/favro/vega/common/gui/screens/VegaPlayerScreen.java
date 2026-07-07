package ca.favro.vega.common.gui.screens;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import ca.favro.vega.common.gui.components.VegaPlayerList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class VegaPlayerScreen extends Screen {
    private static final Component TITLE = Component.literal("Vega Player Management");
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
    private static final Identifier SEARCH_SPRITE = Identifier.withDefaultNamespace("icon/search");
    private static final Component TAB_NEUTRAL = Component.literal("Neutral");
    private static final Component TAB_ALLY = Component.literal("Ally");
    private static final Component TAB_WATCH = Component.literal("Watch");
    private static final Component TAB_ENEMY = Component.literal("Enemy");
    private static final Component TAB_WATCH_SELECTED = TAB_WATCH.plainCopy().withStyle(ChatFormatting.UNDERLINE);
    private static final Component TAB_ALLY_SELECTED = TAB_ALLY.plainCopy().withStyle(ChatFormatting.UNDERLINE);
    private static final Component TAB_ENEMY_SELECTED = TAB_ENEMY.plainCopy().withStyle(ChatFormatting.UNDERLINE);
    private static final Component TAB_NEUTRAL_SELECTED = TAB_NEUTRAL.plainCopy().withStyle(ChatFormatting.UNDERLINE);
    private static final Component SEARCH_HINT = Component.translatable("gui.socialInteractions.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
    static final Component EMPTY_SEARCH = Component.translatable("gui.socialInteractions.search_empty").withStyle(ChatFormatting.GRAY);
    private static final int BG_BORDER_SIZE = 8;
    private static final int BG_WIDTH = 236;
    private static final int SEARCH_HEIGHT = 16;
    private static final int MARGIN_Y = 64;
    public static final int SEARCH_START = 72;
    public static final int LIST_START = 88;
    private static final int IMAGE_WIDTH = 238;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 36;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private final Screen lastScreen;
    @Nullable
    VegaPlayerList socialInteractionsPlayerList;
    EditBox searchBox;
    private String lastSearch = "";
    private VegaPlayerScreen.Page page = Page.NEUTRAL;
    private Button watchButton;
    private Button allyButton;
    private Button enemyButton;
    private Button neutralButton;
    @Nullable
    private Component serverLabel;
    private int playerCount;

    public VegaPlayerScreen() {
        this(null);
    }

    public VegaPlayerScreen(@Nullable Screen screen) {
        super(TITLE);
        this.lastScreen = screen;
        this.updateServerLabel(Minecraft.getInstance());
    }

    private int windowHeight() {
        return Math.max(52, this.height - 128 - 16);
    }

    private int listEnd() {
        return 80 + this.windowHeight() - 8;
    }

    private int marginX() {
        return (this.width - 338) / 2;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.socialInteractionsPlayerList = new VegaPlayerList(this, this.minecraft, this.width, this.listEnd() - 88, 88, 36);
        int i = this.socialInteractionsPlayerList.getRowWidth() / 4;
        int j = this.socialInteractionsPlayerList.getRowLeft();
        int k = this.socialInteractionsPlayerList.getRowRight();
        this.neutralButton = this.addRenderableWidget(Button.builder(TAB_NEUTRAL,
                        button -> this.showPage(Page.NEUTRAL)).bounds(j, 45, i, 20)
                .build()
        );
        this.allyButton = this.addRenderableWidget(Button.builder(TAB_ALLY,
                        button -> this.showPage(VegaPlayerScreen.Page.ALLY)).bounds(j + i, 45, i, 20)
                .build()
        );
        this.watchButton = this.addRenderableWidget(Button.builder(TAB_WATCH,
                        button -> this.showPage(VegaPlayerScreen.Page.WATCH)).bounds(j + 2 * i, 45, i, 20)
                .build()
        );
        this.enemyButton = this.addRenderableWidget(Button.builder(TAB_ENEMY,
                        button -> this.showPage(VegaPlayerScreen.Page.ENEMY)).bounds(j + 3 * i, 45, i, 20)
                .build()
        );
        String string = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = this.addRenderableWidget(
                new EditBox(this.font, this.marginX() + 28 + 50, 74, 200, 15, SEARCH_HINT)
        );
        this.searchBox.setMaxLength(16);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(-1);
        this.searchBox.setValue(string);
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setResponder(this::checkSearchStringUpdate);
        this.addWidget(this.socialInteractionsPlayerList);
        this.showPage(this.page);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.socialInteractionsPlayerList.updateSizeAndPosition(this.width, this.listEnd() - 88, 88);
        this.searchBox.setPosition(this.marginX() + 28 + 50, 74);
        int i = this.socialInteractionsPlayerList.getRowLeft();
        int k = this.socialInteractionsPlayerList.getRowWidth() / 4;
        this.neutralButton.setPosition(i, 45);
        this.allyButton.setPosition(i + k, 45);
        this.watchButton.setPosition(i + 2 * k, 45);
        this.enemyButton.setPosition(i + 3 * k, 45);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void showPage(VegaPlayerScreen.Page page) {
        Vega vega = Vega.getInstance();
        this.page = page;

        this.neutralButton.setMessage(TAB_NEUTRAL);
        this.allyButton.setMessage(TAB_ALLY);
        this.watchButton.setMessage(TAB_WATCH);
        this.enemyButton.setMessage(TAB_ENEMY);
        Set<UUID> set;
        switch (page) {
            case NEUTRAL:
                this.neutralButton.setMessage(TAB_NEUTRAL_SELECTED);
                set = new HashSet<>(this.minecraft.player.connection.getOnlinePlayerIds());
                set.addAll(vega.getVegaUsers().keySet());
                this.socialInteractionsPlayerList.updatePlayerList(set, this.socialInteractionsPlayerList.scrollAmount(), true);
                break;
            case ALLY:
                this.allyButton.setMessage(TAB_ALLY_SELECTED);
                set = new HashSet<>(
                        vega.getVegaUsers().values().stream().filter(
                                vegaUser -> vegaUser.status() == 1
                        ).map(VegaUser::uuid).toList()
                );
                this.socialInteractionsPlayerList.updatePlayerList(set, this.socialInteractionsPlayerList.scrollAmount(), false);
                break;
            case WATCH:
                this.watchButton.setMessage(TAB_WATCH_SELECTED);
                set = new HashSet<>(
                        vega.getVegaUsers().values().stream().filter(
                                vegaUser -> vegaUser.status() == 2
                        ).map(VegaUser::uuid).toList()
                );
                this.socialInteractionsPlayerList.updatePlayerList(set, this.socialInteractionsPlayerList.scrollAmount(), false);
                break;
            case ENEMY:
                this.enemyButton.setMessage(TAB_ENEMY_SELECTED);
                set = new HashSet<>(
                        vega.getVegaUsers().values().stream().filter(
                                vegaUser -> vegaUser.status() == 3
                        ).map(VegaUser::uuid).toList()
                );
                this.socialInteractionsPlayerList.updatePlayerList(set, this.socialInteractionsPlayerList.scrollAmount(), false);
                break;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
        int k = this.marginX() + 3;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, k, 64, 336, this.windowHeight() + 16);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SEARCH_SPRITE, k + 10 + 50, 76, 12, 12);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        this.updateServerLabel(this.minecraft);
        if (this.serverLabel != null) {
            guiGraphics.drawString(this.minecraft.font, this.serverLabel, this.marginX() + 8, 35, -1);
        }

        if (!this.socialInteractionsPlayerList.isEmpty()) {
            this.socialInteractionsPlayerList.render(guiGraphics, i, j, f);
        } else if (!this.searchBox.getValue().isEmpty()) {
            guiGraphics.drawCenteredString(this.minecraft.font, EMPTY_SEARCH, this.width / 2, (72 + this.listEnd()) / 2, -1);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.searchBox.isFocused() && this.minecraft.options.keySocialInteractions.matches(keyEvent)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(keyEvent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void checkSearchStringUpdate(String string) {
        string = string.toLowerCase(Locale.ROOT);
        if (!string.equals(this.lastSearch)) {
            this.socialInteractionsPlayerList.setFilter(string);
            this.lastSearch = string;
            this.showPage(this.page);
        }
    }

    private void updateServerLabel(Minecraft minecraft) {
        int i = minecraft.getConnection().getOnlinePlayers().size();
        if (this.playerCount != i) {
            String string = "";
            ServerData serverData = minecraft.getCurrentServer();
            if (minecraft.isLocalServer()) {
                string = minecraft.getSingleplayerServer().getMotd();
            } else if (serverData != null) {
                string = serverData.name;
            }

            if (i > 1) {
                this.serverLabel = Component.translatable("gui.socialInteractions.server_label.multiple", string, i);
            } else {
                this.serverLabel = Component.translatable("gui.socialInteractions.server_label.single", string, i);
            }

            this.playerCount = i;
        }
    }

    public void onAddPlayer(PlayerInfo playerInfo) {
        this.socialInteractionsPlayerList.addPlayer(playerInfo, this.page);
    }

    public void onRemovePlayer(UUID uUID) {
        this.socialInteractionsPlayerList.removePlayer(uUID);
    }

    @Environment(EnvType.CLIENT)
    public enum Page {
        NEUTRAL,
        ALLY,
        WATCH,
        ENEMY
    }
}
