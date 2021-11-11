package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
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
    private static volatile Task CLEANER_TASK = null;

    static {
        MinecraftServer.getSchedulerManager().buildShutdownTask(ServerDiscovery::stop).schedule();
    }

    private static final Map<SocketAddress, MenuServerInfo> NETWORK_SERVERS = new HashMap<>();
    private static final Map<SocketAddress, Long> SERVERS_UPDATES = new HashMap<>();

    private ServerDiscovery() {}

    public static boolean start() {
        if (RECEIVER_SOCKET != null) return false;
        try {
            RECEIVER_SOCKET = new MulticastSocket(MULTICAST_ADDRESS.getPort());
            RECEIVER_SOCKET.setSoTimeout(1000);
            LOGGER.info("Multicast Receiver running at:" + RECEIVER_SOCKET.getLocalSocketAddress());
            RECEIVER_SOCKET.joinGroup(MULTICAST_ADDRESS.getAddress());
        } catch (IOException e) {
            LOGGER.warn("Could not bind to the port!", e);
            return false;
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

                try {
                    MenuServerInfo networkServer = GSON.fromJson(message, MenuServerInfo.class);
                    InetSocketAddress address = new InetSocketAddress(
                            networkServer.getAddress().getHost(),
                            networkServer.getAddress().getPort());
                    NETWORK_SERVERS.put(address, networkServer);
                    SERVERS_UPDATES.put(address, System.currentTimeMillis());
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Couldn't decode JSON object!", e);
                }
            }
        });

        RECEIVER_THREAD.start();

        CLEANER_TASK = MinecraftServer.getSchedulerManager().buildTask(() -> {
            long expirationTime = System.currentTimeMillis() - 3000;
            Set<SocketAddress> expiredAddresses = new HashSet<>();
            for (Map.Entry<SocketAddress, Long> lastUpdate : SERVERS_UPDATES.entrySet()) {
                if (lastUpdate.getValue() < expirationTime) {
                    expiredAddresses.add(lastUpdate.getKey());
                }
            }
            expiredAddresses.forEach(socketAddress -> {
                SERVERS_UPDATES.remove(socketAddress);
                NETWORK_SERVERS.remove(socketAddress);
            });
        }).repeat(Duration.ofSeconds(1)).schedule();

        return true;
    }

    public static boolean stop() {
        if (RECEIVER_SOCKET == null) return false;

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

        return true;
    }

    public static @Unmodifiable Map<SocketAddress, MenuServerInfo> getNetworkServers() {
        return Map.copyOf(NETWORK_SERVERS);
    }
}
