package net.chrotos.ingress.minecraft.gamemode;

public class GameModeMap {
    private boolean required;
    private String name;
    private MavenLikeDependency dependency;

    public boolean isRequired() {
        return required;
    }

    public String getName() {
        return name;
    }

    public MavenLikeDependency getDependency() {
        return dependency;
    }
}
