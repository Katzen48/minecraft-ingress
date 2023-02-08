package net.chrotos.ingress.minecraft.bungeecord;

import net.chrotos.ingress.minecraft.Watcher;
import net.chrotos.ingress.minecraft.gamemode.GameMode;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;
import java.net.InetSocketAddress;

public class IngressPlugin extends Plugin {
    private Watcher watcher;

    @Override
    public void onEnable() {
        try {
            watcher = new Watcher((endpoint, deleted, watcher) -> {
                String fullname = endpoint.getPodNamespace() + "/" + endpoint.getPodName();

                if (deleted) {
                    getLogger().info(String.format("Server %s will be removed.", fullname));
                    getProxy().getServers().remove(fullname);
                    getProxy().getConfigurationAdapter().getListeners().forEach(listenerInfo -> {
                        listenerInfo.getServerPriority().remove(fullname);
                    });
                } else {
                    getLogger().info(String.format("New Server %s will be added.", fullname));

                    getProxy().getServers().put(fullname, getProxy().constructServerInfo(
                            fullname, new InetSocketAddress(endpoint.getAddresses()[0],endpoint.getPorts().get(0)),
                            "",
                            false));

                    boolean tryServer = watcher.getGameModes().stream()
                            .filter(gameMode -> gameMode.areNameAndNamespaceEqual(endpoint.getGameMode(), endpoint.getPodNamespace()))
                            .findFirst()
                            .map(GameMode::isLobby)
                            .orElse(false);
                    if (tryServer) {
                        getProxy().getConfigurationAdapter().getListeners().forEach(listenerInfo -> {
                            listenerInfo.getServerPriority().add(fullname);
                        });
                    }
                }
            }, (gameMode, deleted) -> {
                // TODO
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
