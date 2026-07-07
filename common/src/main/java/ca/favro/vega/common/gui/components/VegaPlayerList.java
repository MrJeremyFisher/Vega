package ca.favro.vega.common.gui.components;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.gui.screens.VegaPlayerScreen;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class VegaPlayerList extends ContainerObjectSelectionList<VegaPlayerEntry> {
    private final VegaPlayerScreen vegaPlayerScreen;
    private final List<VegaPlayerEntry> players = Lists.newArrayList();
    @Nullable
    private String filter;

    public VegaPlayerList(VegaPlayerScreen vegaPlayerScreen, Minecraft minecraft, int i, int j, int k, int l) {
        super(minecraft, i, j, k, l);
        this.vegaPlayerScreen = vegaPlayerScreen;
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }

    @Override
    protected void enableScissor(GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
    }

    public void updatePlayerList(Collection<UUID> collection, double d, boolean bl) {
        Map<UUID, VegaPlayerEntry> map = new HashMap();
        this.addOnlinePlayers(collection, map);
//        this.updatePlayersFromChatLog(map, bl);
        this.updateFiltersAndScroll(map.values(), d);
    }

    private void addOnlinePlayers(Collection<UUID> collection, Map<UUID, VegaPlayerEntry> map) {
        Vega vega = Vega.getInstance();
        ClientPacketListener clientPacketListener = this.minecraft.player.connection;

        for (UUID uUID : collection) {
            PlayerInfo playerInfo = clientPacketListener.getPlayerInfo(uUID);
            if (playerInfo != null) {
                VegaPlayerEntry playerEntry = this.makePlayerEntry(uUID, playerInfo, true);
                map.put(uUID, playerEntry);
            } else if (vega.getVegaUsers().containsKey(uUID)) {
                map.put(uUID, this.makePlayerEntry(uUID,
                        new PlayerInfo(new GameProfile(uUID, vega.getVegaUsers().get(uUID).name()), false), false
                ));
            }
        }
    }

    private VegaPlayerEntry makePlayerEntry(UUID uUID, PlayerInfo playerInfo, boolean isOnline) {
        return new VegaPlayerEntry(
                this.minecraft, uUID, playerInfo.getProfile().name(),
                playerInfo::getSkin,
                isOnline
        );
    }

    private void updatePlayersFromChatLog(Map<UUID, VegaPlayerEntry> map, boolean bl) {
        Map<UUID, GameProfile> map2 = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
        map2.forEach(
                (uUID, gameProfile) -> {
                    if (bl) {
                        map.computeIfAbsent(
                                uUID,
                                uUIDx -> {
                                    VegaPlayerEntry playerEntryx = new VegaPlayerEntry(
                                            this.minecraft,
                                            gameProfile.id(),
                                            gameProfile.name(),
                                            this.minecraft.getSkinManager().createLookup(gameProfile, false),
                                            true
                                    );
                                    playerEntryx.setOnline(true);
                                    return playerEntryx;
                                }
                        );
                    }
                }
        );
    }

    private static Map<UUID, GameProfile> collectProfilesFromChatLog(ChatLog chatLog) {
        Map<UUID, GameProfile> map = new Object2ObjectLinkedOpenHashMap<>();

        for (int i = chatLog.end(); i >= chatLog.start(); i--) {
            if (chatLog.lookup(i) instanceof LoggedChatMessage.Player player && player.message().hasSignature()) {
                map.put(player.profileId(), player.profile());
            }
        }

        return map;
    }

    private void sortPlayerEntries() {
//
//
//        this.players.sort(Comparator.comparing((VegaPlayerEntry) playerEntry -> {
//            if (this.minecraft.isLocalPlayer(playerEntry.getPlayerId())) {
//                return 0;
//            }
//        });
    }

    private void updateFiltersAndScroll(Collection<VegaPlayerEntry> collection, double d) {
        this.players.clear();
        this.players.addAll(collection);
        this.sortPlayerEntries();
        this.updateFilteredPlayers();
        this.replaceEntries(this.players);
        this.setScrollAmount(d);
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    private void updateFilteredPlayers() {
        if (this.filter != null) {
            this.players.removeIf(playerEntry -> !playerEntry.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
            this.replaceEntries(this.players);
        }
    }

    public void setFilter(String string) {
        this.filter = string;
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public void addPlayer(PlayerInfo playerInfo, VegaPlayerScreen.Page page) {
        UUID uUID = playerInfo.getProfile().id();

        for (VegaPlayerEntry playerEntry : this.players) {
            if (playerEntry.getPlayerId().equals(uUID)) {
                playerEntry.setOnline(false);
                return;
            }
        }

        if ((page == VegaPlayerScreen.Page.NEUTRAL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uUID))
                && (Strings.isNullOrEmpty(this.filter) || playerInfo.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
            boolean bl = playerInfo.hasVerifiableChat();
            VegaPlayerEntry playerEntryx = new VegaPlayerEntry(
                    this.minecraft, playerInfo.getProfile().id(), playerInfo.getProfile().name(), playerInfo::getSkin, bl
            );
            this.addEntry(playerEntryx);
            this.players.add(playerEntryx);
        }
    }

    public void removePlayer(UUID uUID) {
        for (VegaPlayerEntry playerEntry : this.players) {
            if (playerEntry.getPlayerId().equals(uUID)) {
                playerEntry.setOnline(true);
                return;
            }
        }
    }
}
