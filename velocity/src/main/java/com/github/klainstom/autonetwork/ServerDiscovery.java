package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ServerDiscovery {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final InetSocketAddress MULTICAST_ADDRESS =
            new InetSocketAddress("224.0.2.60", 4446);
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerDiscovery.class);

    private static volatile MulticastSocket RECEIVER_SOCKET = null;
    private static volatile Thread RECEIVER_THREAD = null;
    private static volatile ScheduledTask CLEANER_TASK = null;

    private static final Map<InetSocketAddress, BasicServerInfo> NETWORK_SERVERS = new HashMap<>();
    private static final Map<InetSocketAddress, Long> SERVERS_UPDATES = new HashMap<>();

    private ServerDiscovery() {}

    public static void listen(ProxyServer server, AutoNetworkVelocity plugin) {
        if (RECEIVER_SOCKET != null) return;
        try {
            RECEIVER_SOCKET = new MulticastSocket(MULTICAST_ADDRESS.getPort());
            RECEIVER_SOCKET.setSoTimeout(1000);
            LOGGER.info("Multicast Receiver running at:" + RECEIVER_SOCKET.getLocalSocketAddress());
            RECEIVER_SOCKET.joinGroup(MULTICAST_ADDRESS.getAddress());
        } catch (IOException e) {
            LOGGER.warn("Could not bind to the port!", e);
            return;
        }

        RECEIVER_THREAD = new Thread(() -> {
            while (!Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                try {
                    RECEIVER_SOCKET.receive(packet);
                } catch (SocketTimeoutException e) {
                    if (Thread.interrupted()) return;
                    continue;
                } catch (IOException e) {
                    LOGGER.warn("Could not receive datagram!", e);
                }
                String message = new String(packet.getData(), packet.getOffset(), packet.getLength());

                BasicServerInfo networkServer = GSON.fromJson(message, BasicServerInfo.class);
                InetSocketAddress address = new InetSocketAddress(
                        networkServer.getAddress().getHost(),
                        networkServer.getAddress().getPort());
                NETWORK_SERVERS.put(address, networkServer);
                if (SERVERS_UPDATES.put(address, System.currentTimeMillis()) == null) {
                    server.registerServer(new ServerInfo(networkServer.getId(), address));
                }
            }
        });

        RECEIVER_THREAD.start();

        CLEANER_TASK = server.getScheduler().buildTask(plugin, () -> {
            long expirationTime = System.currentTimeMillis() - 3000;
            Set<InetSocketAddress> expiredAddresses = new HashSet<>();
            for (Map.Entry<InetSocketAddress, Long> lastUpdate : SERVERS_UPDATES.entrySet()) {
                if (lastUpdate.getValue() < expirationTime) {
                    expiredAddresses.add(lastUpdate.getKey());
                }
            }
            expiredAddresses.forEach(socketAddress -> {
                BasicServerInfo networkServer = NETWORK_SERVERS.get(socketAddress);
                server.unregisterServer(new ServerInfo(networkServer.getId(), socketAddress));
                SERVERS_UPDATES.remove(socketAddress);
                NETWORK_SERVERS.remove(socketAddress);
            });
        }).repeat(Duration.ofSeconds(1)).schedule();

    }

    public static void stopListen() {
        if (RECEIVER_SOCKET == null) return;

        RECEIVER_THREAD.interrupt();
        try {
            RECEIVER_THREAD.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            RECEIVER_SOCKET.leaveGroup(MULTICAST_ADDRESS.getAddress());
        } catch (IOException e) {
            LOGGER.warn("Could not leave multicast group!", e);
        }
        RECEIVER_SOCKET.close();
        CLEANER_TASK.cancel();

        RECEIVER_SOCKET = null;
        RECEIVER_THREAD = null;
        CLEANER_TASK = null;
        NETWORK_SERVERS.clear();
        SERVERS_UPDATES.clear();

    }

    public static @Unmodifiable Map<SocketAddress, BasicServerInfo> getNetworkServers() {
        return Map.copyOf(NETWORK_SERVERS);
    }
}
