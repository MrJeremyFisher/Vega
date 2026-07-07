package ca.favro.vega.common.integrations.combatradar;

import ca.favro.vega.common.Vega;
import ca.favro.vega.common.VegaUser;
import com.aleksey.combatradar.Radar;
import com.aleksey.combatradar.config.PlayerType;

public class CombatRadarIntegration {
    public static void syncStatuses() {
        Vega.getInstance().getVegaUsers().forEach(((uuid, vegaUser) -> {
            setPlayerType(vegaUser);
        }));
    }

    public static void syncStatus(VegaUser vegaUser) {
        setPlayerType(vegaUser);
    }

    private static void setPlayerType(VegaUser vegaUser) {
        PlayerType playerType = switch (vegaUser.status()) {
            case 0, 2 -> PlayerType.Neutral;
            case 1 -> PlayerType.Ally;
            case 3 -> PlayerType.Enemy;
            default -> throw new IllegalStateException("Unexpected value: " + vegaUser.status());
        };
        Radar.getConfig().setPlayerType(vegaUser.name(), playerType);
    }
}
