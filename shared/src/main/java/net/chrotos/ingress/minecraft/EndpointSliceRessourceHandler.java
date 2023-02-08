package net.chrotos.ingress.minecraft;

@FunctionalInterface
public interface EndpointSliceRessourceHandler {
    void onEventReceived(EndpointSlice endpointSlice, boolean deleted) throws Throwable;
}
