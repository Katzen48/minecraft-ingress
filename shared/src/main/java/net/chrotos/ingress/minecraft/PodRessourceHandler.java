package net.chrotos.ingress.minecraft;

@FunctionalInterface
public interface PodRessourceHandler {
    void onEventReceived(Pod pod, boolean deleted) throws Throwable;
}
