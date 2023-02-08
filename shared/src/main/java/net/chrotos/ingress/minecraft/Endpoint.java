package net.chrotos.ingress.minecraft;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Endpoint;
import io.kubernetes.client.openapi.models.V1Pod;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

public class Endpoint {
    private final InetAddress[] addresses;
    private final boolean ready;
    private final String podName;
    private final String podNamespace;
    private final String nodeName;
    private final List<Integer> ports;
    private String gameMode = null;

    public Endpoint(V1Endpoint endpoint, EndpointSlice endpointSlice, CoreV1Api api) {
        this.ports = endpointSlice.getPorts();

        // Translate addresses
        this.addresses = new InetAddress[endpoint.getAddresses().size()];
        for (int i = 0; i < addresses.length; i++) {
            try {
                this.addresses[i] = InetAddress.getByName(endpoint.getAddresses().get(i));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Get ready condition
        if (endpoint.getConditions() == null || endpoint.getConditions().getReady() == null) {
            this.ready = false;
        } else {
            this.ready = endpoint.getConditions().getReady();
        }

        // Get Pod name and namespace
        if (endpoint.getTargetRef() == null || endpoint.getTargetRef().getKind() == null ||
            endpoint.getTargetRef().getKind() == null ||  endpoint.getTargetRef().getName() == null ||
            endpoint.getTargetRef().getNamespace() == null ||
            !endpoint.getTargetRef().getKind().equalsIgnoreCase("Pod")) {
                this.podName = endpoint.getTargetRef().getName();
                this.podNamespace = endpoint.getTargetRef().getNamespace();

                try {
                    V1Pod pod = api.readNamespacedPod(this.podName, this.podNamespace, null);
                    if (pod.getMetadata() != null && pod.getMetadata().getLabels() != null) {
                        this.gameMode = pod.getMetadata().getLabels().get("net.chrotos.chrotoscloud.gameserver/gamemode");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {
            this.podName = null;
            this.podNamespace = null;
        }

        this.nodeName = endpoint.getNodeName();
    }

    public InetAddress[] getAddresses() {
        return addresses;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public boolean isReady() {
        return ready && this.podName != null && this.podNamespace != null && addresses.length > 0;
    }

    public String getPodName() {
        return podName;
    }

    public String getPodNamespace() {
        return podNamespace;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getGameMode() {
        return gameMode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Endpoint endpoint)) {
            return false;
        }

        return Objects.equals(endpoint.podName, podName) && Objects.equals(endpoint.podNamespace, podNamespace);
    }
}
