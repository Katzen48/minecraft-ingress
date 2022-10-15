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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Watcher {
    private final SharedInformerFactory factory;

    public Watcher(PodRessourceHandler handler) throws IOException {
        ApiClient apiClient = Config.defaultClient();
        Configuration.setDefaultApiClient(apiClient);

        CoreV1Api v1Api = new CoreV1Api();
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);

        factory = new SharedInformerFactory(apiClient);
        ResourceEventHandler<V1Pod> resourceEventHandler = new ResourceEventHandler<>() {
            @Override
            public void onAdd(V1Pod obj) {
                if (obj.getStatus() == null || obj.getStatus().getContainerStatuses() == null) {
                    return;
                }
                // TODO change to service lookup
                Optional<V1ContainerStatus> status = getServerStatus(obj);

                try {
                    if (status.isPresent() && status.get().getReady()) {
                        handler.onEventReceived(new Pod(obj, v1Api), false);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onUpdate(V1Pod oldObj, V1Pod newObj) {
                if (oldObj.getStatus() == null || newObj.getStatus() == null
                        || oldObj.getStatus().getContainerStatuses() == null
                        || newObj.getStatus().getContainerStatuses() == null) {
                    return;
                }

                String containerName = getServerContainerName(newObj);
                if (containerName == null) {
                    containerName = getServerContainerName(oldObj);
                }

                if (containerName == null) {
                    return;
                }

                Optional<V1ContainerStatus> oldStatus = getServerStatus(oldObj, containerName);
                Optional<V1ContainerStatus> newStatus = getServerStatus(newObj, containerName);

                boolean oldReady = oldStatus.isPresent() && oldStatus.get().getReady();
                boolean newReady = newStatus.isPresent() && newStatus.get().getReady();

                try {
                    if (!oldReady && newReady) {
                        handler.onEventReceived(new Pod(newObj, v1Api), false);
                    }

                    if (oldReady && !newReady) {
                        Pod pod = new Pod(oldObj, v1Api);
                        handler.onEventReceived(pod, true);
                        pod.delete();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) { }
        };

        String namespaces = System.getenv("INGRESS_NAMESPACES");
        if (namespaces == null || namespaces.isEmpty()) {
            registerInformer(null, v1Api, resourceEventHandler);
        } else {
            for (String namespace : namespaces.split(",")) {
                registerInformer(namespace, v1Api, resourceEventHandler);
            }
        }
    }

    private void registerInformer(String namespace, CoreV1Api v1Api, ResourceEventHandler<V1Pod> handler) {
        SharedIndexInformer<V1Pod> podInformer;
        if (namespace == null) {
         podInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> v1Api.listPodForAllNamespacesCall(
                            null,
                            null,
                            null,
                            "net.chrotos.ingress.minecraft/discover=true",
                            null,
                            null,
                            params.resourceVersion,
                            null,
                            params.timeoutSeconds,
                            params.watch,
                            null),
                    V1Pod.class,
                    V1PodList.class);
        } else {
            podInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> v1Api.listNamespacedPodCall(
                            namespace,
                            null,
                            null,
                            null,
                            null,
                            "net.chrotos.ingress.minecraft/discover=true",
                            null,
                            params.resourceVersion,
                            null,
                            params.timeoutSeconds,
                            params.watch,
                            null),
                    V1Pod.class,
                    V1PodList.class);
        }

        podInformer.addEventHandler(handler);
    }

    private Optional<V1ContainerStatus> getServerStatus(@NonNull V1Pod obj) {
        String containerName = getServerContainerName(obj);

        return getServerStatus(obj, containerName);
    }

    private Optional<V1ContainerStatus> getServerStatus(@NonNull V1Pod obj, String containerName) {
        if (containerName == null || obj.getStatus() == null || obj.getStatus().getContainerStatuses() == null) {
            return Optional.empty();
        }

       return obj.getStatus().getContainerStatuses().stream()
                .filter(stat -> stat.getName() != null && stat.getName().equals(containerName)).findFirst();
    }

    private String getServerContainerName(@NonNull V1Pod obj) { // TODO change to service lookup
        if (obj.getSpec() == null || obj.getSpec().getContainers() == null) {
            return null;
        }

        // Find container
        return obj.getSpec().getContainers().stream().filter(cont ->
                // Find by port
                (cont.getPorts() != null &&
                    cont.getPorts().stream().anyMatch(port ->
                            (port.getName() != null && port.getName().equalsIgnoreCase("minecraft")) ||
                                    (port.getContainerPort() != null && port.getContainerPort() == 25565))) ||
                // Find by name
                (cont.getName() != null && (cont.getName().equalsIgnoreCase("paper") ||
                        cont.getName().equalsIgnoreCase("spigot") ||
                        cont.getName().equalsIgnoreCase("bukkit")))
        ).findFirst().map(V1Container::getName).orElse(null);

    }

    public void start() {
        factory.startAllRegisteredInformers();
    }

    public void stop() {
        factory.stopAllRegisteredInformers();
    }
}
