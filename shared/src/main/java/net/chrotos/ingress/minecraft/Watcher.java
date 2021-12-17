package net.chrotos.ingress.minecraft;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Watcher {
    private SharedInformerFactory factory;

    public Watcher(PodRessourceHandler handler) throws IOException {
        CoreV1Api v1Api = new CoreV1Api();
        ApiClient apiClient = v1Api.getApiClient();
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);

        factory = new SharedInformerFactory();
        SharedIndexInformer<V1Pod> podInformer = factory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> {
                    return v1Api.listNamespacedPodCall(
                            null,
                            null,
                            null,
                            null,
                            null,
                            "net.chrotos.ingress.minecraft/discover=true",
                            null,
                            params.resourceVersion,
                            params.timeoutSeconds,
                            params.watch,
                            null);
                },
                V1Pod.class,
                V1PodList.class);

        podInformer.addEventHandler(
                new ResourceEventHandler<>() {
                    @Override
                    public void onAdd(V1Pod obj) { }

                    @Override
                    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
                        Stream<V1ContainerStatus> oldStream = Objects.requireNonNull(Objects.requireNonNull(
                                                            oldObj.getStatus()).getContainerStatuses()).stream()
                                                            .filter(V1ContainerStatus::getReady);

                        Stream<V1ContainerStatus> newStream = Objects.requireNonNull(Objects.requireNonNull(
                                newObj.getStatus()).getContainerStatuses()).stream()
                                .filter(V1ContainerStatus::getReady);

                        if (!oldStream.findAny().isPresent() && newStream.findAny().isPresent()) {
                            handler.onEventReceived(newObj, false);
                        }

                        if (oldStream.findAny().isPresent() && !newStream.findAny().isPresent()) {
                            handler.onEventReceived(oldObj, true);
                        }
                    }

                    @Override
                    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) { }
                }
        );
    }

    public void start() {
        factory.startAllRegisteredInformers();
    }

    public void stop() {
        factory.stopAllRegisteredInformers();
    }
}
