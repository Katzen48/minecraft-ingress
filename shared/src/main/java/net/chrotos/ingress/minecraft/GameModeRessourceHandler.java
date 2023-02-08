package net.chrotos.ingress.minecraft;

import net.chrotos.ingress.minecraft.gamemode.GameMode;

@FunctionalInterface
public interface GameModeRessourceHandler {
    void onEventReceived(GameMode gameMode, boolean deleted) throws Throwable;
}
