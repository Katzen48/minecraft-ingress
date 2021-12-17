package net.chrotos.ingress.minecraft.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.chrotos.ingress.minecraft.Watcher;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

@Plugin(id="ingress-plugin", name = "Kubernetes Ingress Plugin", version = "1.0", authors = {"Katzen48"})
public class IngressPlugin {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private Watcher watcher;

    @Inject
    public IngressPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            watcher = new Watcher((pod, deleted) -> {
                String name = Objects.requireNonNull(pod.getMetadata()).getName();

                if (deleted) {
                    logger.info("Server {} will be removed.", name);
                    proxyServer.getServer(name).ifPresent(registeredServer -> proxyServer.unregisterServer(registeredServer.getServerInfo()));
                } else {
                    logger.info("New Server {} will be added.", name);

                    proxyServer.registerServer(new ServerInfo(name, InetSocketAddress.createUnresolved(Objects.requireNonNull(
                            Objects.requireNonNull(pod.getStatus())
                                    .getPodIP()),
                            25565)));
                }
            });

            watcher.start();
        } catch (IOException e) {
            e.printStackTrace();
            proxyServer.shutdown();
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        watcher.stop();
    }
}
