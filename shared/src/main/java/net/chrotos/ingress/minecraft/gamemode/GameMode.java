package net.chrotos.ingress.minecraft.gamemode;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

import java.util.List;
import java.util.Objects;

public class GameMode implements KubernetesObject {
    @SerializedName("apiVersion")
    private String apiVersion;
    @SerializedName("kind")
    private String kind;
    @SerializedName("metadata")
    private V1ObjectMeta metadata = null;
    @SerializedName("spec")
    private GameModeSpec spec;

    public String getVersion() {
        return spec.getVersion();
    }

    public String getCloudVersion() {
        return spec.getCloudVersion();
    }

    public GameModeMaps getMaps() {
        return spec.getMaps();
    }

    public List<GameModePlugin> getPlugins() {
        return spec.getPlugins();
    }

    public GameModeResourcePack getResourcePack() {
        return spec.getResourcePack();
    }

    public boolean isLobby() {
        return spec.isLobby();
    }

    public String getName() {
        return metadata.getName();
    }

    public String getNamespace() {
        return metadata.getNamespace();
    }

    @Override
    public V1ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public boolean areNameAndNamespaceEqual(String name, String namespace) {
        return Objects.equals(name, getName()) && Objects.equals(namespace, getNamespace());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GameMode gameMode)) {
            return false;
        }

        return areNameAndNamespaceEqual(gameMode.getName(), gameMode.getNamespace());
    }
}
