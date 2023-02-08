package net.chrotos.ingress.minecraft.gamemode;

public class GameModePlugin {
    private MavenDependency dependency;
    private MavenLikeDependency configuration;

    public MavenDependency getDependency() {
        return dependency;
    }

    public MavenLikeDependency getConfiguration() {
        return configuration;
    }
}
