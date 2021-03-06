package net.chrotos.ingress.minecraft;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Watcher {
    private SharedInformerFactory factory;

    public Watcher(PodRessourceHandler handler) throws IOException {
        ApiClient apiClient = Config.defaultClient();
        Configuration.setDefaultApiClient(apiClient);

        CoreV1Api v1Api = new CoreV1Api();
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);

        factory = new SharedInformerFactory();
        SharedIndexInformer<V1Pod> podInformer = factory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> v1Api.listPodForAllNamespacesCall(
                        null,
                        null,
                        null,
                        "net.chrotos.ingress.minecraft/discover=true",
                        null,
                        null,
                        params.resourceVersion,
                        params.timeoutSeconds,
                        params.watch,
                        null),
                V1Pod.class,
                V1PodList.class);

        podInformer.addEventHandler(
                new ResourceEventHandler<>() {
                    @Override
                    public void onAdd(V1Pod obj) {
                        if (obj.getStatus() == null || obj.getStatus().getContainerStatuses() == null) {
                            return;
                        }

                        Stream<V1ContainerStatus> stream = obj.getStatus().getContainerStatuses().stream()
                                                                            .filter(V1ContainerStatus::getReady);

                        try {
                            if (stream.findAny().isPresent()) {
                                handler.onEventReceived(obj, false);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            stream.close();
                        } finally {
                            stream.close();
                        }
                    }

                    @Override
                    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
                        if (oldObj.getStatus() == null || newObj.getStatus() == null
                                || oldObj.getStatus().getContainerStatuses() == null
                                || newObj.getStatus().getContainerStatuses() == null) {
                            return;
                        }

                        Stream<V1ContainerStatus> oldStream = oldObj.getStatus().getContainerStatuses().stream()
                                                                                .filter(V1ContainerStatus::getReady);

                        Stream<V1ContainerStatus> newStream = newObj.getStatus().getContainerStatuses().stream()
                                                                                .filter(V1ContainerStatus::getReady);

                        boolean oldPresent = oldStream.findAny().isPresent();
                        boolean newPresent = newStream.findAny().isPresent();

                        try {
                            if (!oldPresent && newPresent) {
                                handler.onEventReceived(newObj, false);
                            }

                            if (oldPresent && !newPresent) {
                                handler.onEventReceived(oldObj, true);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            oldStream.close();
                            newStream.close();
                        } finally {
                            oldStream.close();
                            newStream.close();
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
