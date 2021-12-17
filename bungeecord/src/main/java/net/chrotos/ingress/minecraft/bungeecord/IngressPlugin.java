package net.chrotos.ingress.minecraft.bungeecord;

import net.chrotos.ingress.minecraft.Watcher;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

public class IngressPlugin extends Plugin {
    private Watcher watcher;

    @Override
    public void onEnable() {
        try {
            watcher = new Watcher((pod, deleted) -> {
                String name = Objects.requireNonNull(pod.getMetadata()).getName();

                if (deleted) {
                    getLogger().info(String.format("Server %s will be removed.", name));
                    getProxy().getServers().remove(name);
                } else {
                    getLogger().info(String.format("New Server %s will be added.", name));

                    getProxy().getServers().put(name, getProxy().constructServerInfo(
                                name, InetSocketAddress.createUnresolved(Objects.requireNonNull(
                                                                                Objects.requireNonNull(pod.getStatus())
                                                                                .getPodIP()),
                                                                    25565),
                            "",
                            false));
                }
            });

            watcher.start();
        } catch (IOException e) {
            e.printStackTrace();
            getProxy().stop();
        }
    }

    @Override
    public void onDisable() {
        watcher.stop();
    }
}
