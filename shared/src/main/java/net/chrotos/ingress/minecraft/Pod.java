package net.chrotos.ingress.minecraft;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;

public class Pod {
    private final V1Pod pod;
    private final CoreV1Api api;

    protected Pod(V1Pod pod, CoreV1Api api) {
        this.pod = pod;
        this.api = api;
    }

    public V1Pod getPod() {
        return this.pod;
    }

    public void delete() {
        try {
            api.deleteNamespacedPod(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), null, null,
                    null, null, null, null);
        } catch (ApiException ignored) {}
    }
}
