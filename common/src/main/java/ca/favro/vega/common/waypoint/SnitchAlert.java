package ca.favro.vega.common.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SnitchAlert {
    /**
     * ms since UNIX epoch when alert happened
     */
    public final long ts;
    public final @NotNull Vec3 pos;
    public final @NotNull String world;
    public final @NotNull String action;
    public final @NotNull String accountName;
    public final @NotNull String snitchName;
    public final @Nullable String group;

    public SnitchAlert(
            long ts,
            @NotNull Vec3 pos,
            @NotNull String world,
            @NotNull String action,
            @NotNull String accountName,
            @NotNull String snitchName,
            @Nullable String group
    ) {
        this.ts = ts;
        this.pos = pos;
        this.world = world;
        this.action = action;
        this.accountName = accountName;
        this.snitchName = snitchName;
        this.group = group;
    }

    // Enter  PLAYER  SNITCHNAME  [123 45 -321]  [12m North West]
    static Pattern alertPattern = Pattern.compile("^(Enter|Login|Logout) +([A-Za-z0-9_]{3,17}) +(.+) +\\[(?:([A-Za-z][^ ]+),? )?([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\].*");
    // §6Location: §b(world) [123 45 -321]\n§6Name: §bSNITCHNAME\n§6Group: §bGROUPNAME
    static Pattern hoverPattern = Pattern.compile("Location: (?:\\(?([^\\n)]+)\\)? )?\\[([-0-9]+),? ([-0-9]+),? ([-0-9]+)\\] *\\n(?:Name: ([^\\n]+)\\n)?Group: ([^ ]+).*", Pattern.MULTILINE);

    @Nullable
    public static SnitchAlert fromChat(
            @NotNull Component message,
            @NotNull String world
    ) {
        String text = message.getString().replaceAll("§.", "");

        Matcher textMatch = alertPattern.matcher(text);
        if (!textMatch.matches()) return null;

        String action = textMatch.group(1);
        String accountName = textMatch.group(2);
        String snitchName = textMatch.group(3);
        world = textMatch.group(4) == null || textMatch.group(4).isEmpty() ? world : textMatch.group(4);
        int x = Integer.parseInt(textMatch.group(5));
        int y = Integer.parseInt(textMatch.group(6));
        int z = Integer.parseInt(textMatch.group(7));

        String group = null;
        final HoverEvent hoverEvent = message.getSiblings().getFirst().getStyle().getHoverEvent();
        if (hoverEvent != null && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
            String hoverText = ((HoverEvent.ShowText) hoverEvent).value().getString().replaceAll("§.", "");

            Matcher hoverMatch = hoverPattern.matcher(hoverText);
            if (hoverMatch.matches()) {
                world = hoverMatch.group(1) == null || hoverMatch.group(1).isEmpty() ? world : hoverMatch.group(1);
                group = hoverMatch.group(6);
            }
        }

        world = switch (world) {
            case "world" -> "overworld";
            case "world_nether" -> "the_nether";
            case "world_the_end" -> "the_end";
            default -> world;
        };

        long now = System.currentTimeMillis();

        Vec3 pos = new Vec3(x, y, z);

        return new SnitchAlert(now, pos, world, action, accountName, snitchName, group);
    }
}
