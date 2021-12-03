package com.github.klainstom.autonetwork;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

@Plugin(
        id = "auto-network-velocity",
        name = "AutoNetwork",
        version = BuildConstants.VERSION,
        description = "Automatically register servers using UDP multicast",
        authors = {"offby0point5"}
)
public class AutoNetworkVelocity {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNetworkVelocity.class);

    public static ProxyServer SERVER;

    @Inject
    public AutoNetworkVelocity(ProxyServer server) {
        SERVER = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ServerDiscovery.listen(SERVER, this);
        LOGGER.info("Start listening to server promotion messages.");
        SERVER.getCommandManager().register("l", new LobbyCommand());
    }

    @Subscribe
    public void onProxyStart(ListenerBoundEvent event) {
        SERVER.getServer("noserver").ifPresent(server -> SERVER.unregisterServer(server.getServerInfo()));

        Via.getManager().getProviders().register(VersionProvider.class, new AutoVersionProvider());
        LOGGER.info("Replaced viaversion's version provider.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        ServerDiscovery.stopListen();
        LOGGER.info("Stop listening to server promotion messages.");
    }

    @Subscribe
    public void onFirstServer(PlayerChooseInitialServerEvent event) {
        for (RegisteredServer registeredServer : SERVER.matchServer("lobby-")) {
            event.setInitialServer(registeredServer);
            break;
        }

    }

    @Subscribe
    public void onServerChange(ServerPreConnectEvent event) {
        InetSocketAddress address = event.getOriginalServer().getServerInfo().getAddress();
        BasicServerInfo networkServer = ServerDiscovery.getNetworkServers().get(address);
        if (networkServer == null) return;
        BasicServerInfo.Version serverVersion = networkServer.getMinVersion();
        if (serverVersion == null) serverVersion = networkServer.getVersion();
        if (event.getPlayer().getProtocolVersion().getProtocol() < serverVersion.getProtocol()) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.translatable(
                    "multiplayer.disconnect.outdated_client",
                    NamedTextColor.YELLOW,
                    List.of(Component.text(networkServer.getMinVersion().getName()))));
        }
    }

    @Subscribe
    public void onServerKick(KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect()) {
            event.setResult(KickedFromServerEvent.Notify.create(event.getServerKickReason().orElseGet(
                    () -> Component.translatable("multiplayer.disconnect.generic", NamedTextColor.RED))));
            return;
        }
        InetSocketAddress address = event.getServer().getServerInfo().getAddress();
        BasicServerInfo networkServer = ServerDiscovery.getNetworkServers().get(address);
        if (networkServer == null) return;
        if (networkServer.getGroup().equals("lobby")) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(event.getServerKickReason().orElseGet(
                    () -> Component.translatable("multiplayer.disconnect.generic", NamedTextColor.RED))));
            return;
        }
        for (RegisteredServer registeredServer : SERVER.matchServer("lobby-")) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(registeredServer,
                    event.getServerKickReason().orElseGet(
                            () -> Component.translatable("multiplayer.disconnect.generic", NamedTextColor.RED))));
            break;
        }
    }
}
