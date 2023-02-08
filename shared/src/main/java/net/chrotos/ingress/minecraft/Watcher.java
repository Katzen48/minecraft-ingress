package net.chrotos.ingress.minecraft;

import com.google.common.collect.ArrayListMultimap;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.DiscoveryV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import net.chrotos.ingress.minecraft.gamemode.GameMode;
import net.chrotos.ingress.minecraft.gamemode.GameModeList;
import okhttp3.OkHttpClient;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Watcher {
    private final SharedInformerFactory factory;
    private final ArrayListMultimap<String, Endpoint> endpoints = ArrayListMultimap.create();
    private final HashSet<GameMode> gameModes = new HashSet<>();

    public Watcher(EndpointResourceHandler endpointResourceHandler, GameModeRessourceHandler gameModeRessourceHandler) throws IOException {
        ApiClient apiClient = Config.defaultClient();
        Configuration.setDefaultApiClient(apiClient);

        CoreV1Api v1Api = new CoreV1Api();
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);
        DiscoveryV1Api discoveryV1Api = new DiscoveryV1Api();

        GenericKubernetesApi<GameMode, GameModeList> api = new GenericKubernetesApi<>(GameMode.class, GameModeList.class,
                "chrotoscloud.chrotos.net", "v1", "gamemodes", apiClient);

        factory = new SharedInformerFactory(apiClient);
        ResourceEventHandler<V1Pod> resourceEventHandler = new ResourceEventHandler<>() {
            @Override
            public void onAdd(V1Pod obj) { }

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
                    if (oldReady && !newReady) {
                        Pod pod = new Pod(oldObj, v1Api);
                        pod.delete();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) { }
        };

        ResourceEventHandler<V1EndpointSlice> endpointSliceHandler = new ResourceEventHandler<>() {
            @Override
            public void onAdd(V1EndpointSlice obj) {
                try {
                    EndpointSlice endpointSlice = new EndpointSlice(obj, v1Api);
                    if (!endpointSlice.getReadyEndpoints().isEmpty()) {
                        endpoints.putAll(endpointSlice.getName(), endpointSlice.getReadyEndpoints());
                        endpointSlice.getReadyEndpoints().forEach(endpoint -> {
                            try {
                                endpointResourceHandler.onEventReceived(endpoint, false, Watcher.this);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onUpdate(V1EndpointSlice oldObj, V1EndpointSlice newObj) {
                try {
                    EndpointSlice endpointSlice = new EndpointSlice(newObj, v1Api);

                    List<Endpoint> newEndpoints = new ArrayList<>(endpointSlice.getReadyEndpoints());
                    newEndpoints.removeAll(endpoints.get(endpointSlice.getName()));

                    List<Endpoint> removedEndpoints = new ArrayList<>(endpoints.get(endpointSlice.getName()));
                    removedEndpoints.removeIf(endpoint -> !endpointSlice.getReadyEndpoints().contains(endpoint));

                    newEndpoints.forEach(endpoint -> {
                        try {
                            endpointResourceHandler.onEventReceived(endpoint, false, Watcher.this);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                    removedEndpoints.forEach(endpoint -> {
                        try {
                            endpointResourceHandler.onEventReceived(endpoint, true, Watcher.this);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(V1EndpointSlice obj, boolean deletedFinalStateUnknown) {
                try {
                    EndpointSlice endpointSlice = new EndpointSlice(obj, v1Api);
                    endpoints.removeAll(endpointSlice.getName()).forEach(endpoint -> {
                        try {
                            endpointResourceHandler.onEventReceived(endpoint, true, Watcher.this);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        ResourceEventHandler<GameMode> gameModeHandler = new ResourceEventHandler<GameMode>() {
            @Override
            public void onAdd(GameMode obj) {
                try {
                    gameModeRessourceHandler.onEventReceived(obj, false);
                    gameModes.add(obj);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onUpdate(GameMode oldObj, GameMode newObj) {
                try {
                    gameModeRessourceHandler.onEventReceived(newObj, false);
                    gameModes.add(newObj);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(GameMode obj, boolean deletedFinalStateUnknown) {
                try {
                    gameModeRessourceHandler.onEventReceived(obj, true);
                    gameModes.remove(obj);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        String namespaces = System.getenv("INGRESS_NAMESPACES");
        if (namespaces == null || namespaces.isEmpty()) {
            registerPodInformer(null, v1Api, resourceEventHandler);
            registerEndpointSliceInformer(null, discoveryV1Api, endpointSliceHandler);
            registerGameModeInformer(null, api, gameModeHandler);
        } else {
            for (String namespace : namespaces.split(",")) {
                registerPodInformer(namespace, v1Api, resourceEventHandler);
                registerEndpointSliceInformer(namespace, discoveryV1Api, endpointSliceHandler);
                registerGameModeInformer(namespace, api, gameModeHandler);
            }
        }
    }

    private void registerPodInformer(String namespace, CoreV1Api v1Api, ResourceEventHandler<V1Pod> handler) {
        SharedIndexInformer<V1Pod> podInformer;
        if (namespace == null) {
         podInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> v1Api.listPodForAllNamespacesCall(
                            null,
                            null,
                            null,
                            null,
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
                            null,
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

    private void registerEndpointSliceInformer(String namespace, DiscoveryV1Api discoveryV1Api, ResourceEventHandler<V1EndpointSlice> handler) {
        SharedIndexInformer<V1EndpointSlice> endpointSliceInformer;
        if (namespace == null) {
            endpointSliceInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> discoveryV1Api.listEndpointSliceForAllNamespacesCall(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            params.resourceVersion,
                            null,
                            params.timeoutSeconds,
                            params.watch,
                            null),
                    V1EndpointSlice.class,
                    V1EndpointSliceList.class);
        } else {
            endpointSliceInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> discoveryV1Api.listNamespacedEndpointSliceCall(
                            namespace,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            params.resourceVersion,
                            null,
                            params.timeoutSeconds,
                            params.watch,
                            null),
                    V1EndpointSlice.class,
                    V1EndpointSliceList.class);
        }

        endpointSliceInformer.addEventHandler(handler);
    }

    private void registerGameModeInformer(String namespace, GenericKubernetesApi<GameMode, GameModeList> api, ResourceEventHandler<GameMode> handler) {
        SharedIndexInformer<GameMode> gameModeInformer;
        if (namespace == null) {
            gameModeInformer = factory.sharedIndexInformerFor(api, GameMode.class, 0);
        } else {
            gameModeInformer = factory.sharedIndexInformerFor(api, GameMode.class, 0, namespace);
        }

        gameModeInformer.addEventHandler(handler);
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

    public HashSet<GameMode> getGameModes() {
        return gameModes;
    }
}
