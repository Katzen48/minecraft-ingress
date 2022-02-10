package net.chrotos.ingress.minecraft;

import io.kubernetes.client.openapi.models.V1Pod;

@FunctionalInterface
public interface PodRessourceHandler {
    void onEventReceived(V1Pod pod, boolean deleted) throws Throwable;
}
