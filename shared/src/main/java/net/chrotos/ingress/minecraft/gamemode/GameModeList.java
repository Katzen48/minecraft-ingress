package net.chrotos.ingress.minecraft.gamemode;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.openapi.models.V1ListMeta;

import java.util.ArrayList;
import java.util.List;

public class GameModeList implements KubernetesListObject {
    @SerializedName("apiVersion")
    private String apiVersion;
    @SerializedName("kind")
    private String kind;
    @SerializedName("metadata")
    private V1ListMeta metadata = null;
    @SerializedName("items")
    private List<GameMode> items = new ArrayList<>();

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public V1ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<GameMode> getItems() {
        return items;
    }
}
