package net.chrotos.ingress.minecraft;

public interface EndpointResourceHandler {
    void onEventReceived(Endpoint endpointSlice, boolean deleted, Watcher watcher) throws Throwable;
}
