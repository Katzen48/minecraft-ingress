package net.chrotos.ingress.minecraft.gamemode;

import java.util.ArrayList;
import java.util.List;

public class GameModeMaps {
    private boolean random;
    private List<GameModeMap> pool = new ArrayList<>();

    public boolean isRandom() {
        return random;
    }

    public List<GameModeMap> getPool() {
        return pool;
    }
}
