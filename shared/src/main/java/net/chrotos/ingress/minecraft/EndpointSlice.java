package net.chrotos.ingress.minecraft;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.DiscoveryV1EndpointPort;
import io.kubernetes.client.openapi.models.V1EndpointSlice;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EndpointSlice {
    public static final String APP_PROTOCOL = "com.mojang/minecraft";
    private final V1EndpointSlice endpointSlice;
    private final List<Endpoint> readyEndpoints;
    private final List<Integer> ports;

    protected EndpointSlice(V1EndpointSlice endpointSlice, CoreV1Api api) {
        this.endpointSlice = endpointSlice;

        if (endpointSlice.getPorts() != null) {
            this.ports = endpointSlice.getPorts().stream().filter(port -> APP_PROTOCOL.equals(port.getAppProtocol()) && port.getPort() != null)
                    .map(DiscoveryV1EndpointPort::getPort)
                    .parallel()
                    .collect(Collectors.toList());
        } else {
            this.ports = Collections.emptyList();
        }

        if (!ports.isEmpty()) {
            this.readyEndpoints = endpointSlice.getEndpoints().stream().map(endpoint -> new Endpoint(endpoint, this, api))
                    .filter(Endpoint::isReady)
                    .parallel()
                    .collect(Collectors.toList());
        } else {
            this.readyEndpoints = Collections.emptyList();
        }
    }

    public List<Endpoint> getReadyEndpoints() {
        return readyEndpoints;
    }

    public String getName() {
        return Objects.requireNonNull(endpointSlice.getMetadata()).getName();
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public V1EndpointSlice getEndpointSlice() {
        return this.endpointSlice;
    }
}
