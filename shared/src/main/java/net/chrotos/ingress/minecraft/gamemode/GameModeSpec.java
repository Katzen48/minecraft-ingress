package net.chrotos.ingress.minecraft.gamemode;

import java.util.List;

public class GameModeSpec {
    private String version;
    private String cloudVersion;
    private GameModeMaps maps;
    private List<GameModePlugin> plugins;
    private GameModeResourcePack resourcePack;
    private Boolean lobby;

    public String getVersion() {
        return version;
    }

    public String getCloudVersion() {
        return cloudVersion;
    }

    public GameModeMaps getMaps() {
        return maps;
    }

    public List<GameModePlugin> getPlugins() {
        return plugins;
    }

    public GameModeResourcePack getResourcePack() {
        return resourcePack;
    }

    public Boolean isLobby() {
        return lobby;
    }
}
