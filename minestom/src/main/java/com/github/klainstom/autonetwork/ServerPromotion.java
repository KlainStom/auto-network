package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ServerPromotion {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final InetSocketAddress MULTICAST_ADDRESS =
            new InetSocketAddress("224.0.2.60", 4446);
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPromotion.class);

    private static volatile DatagramSocket SENDER_SOCKET = null;
    private static volatile Task SENDER_TASK = null;

    static {
        MinecraftServer.getSchedulerManager().buildShutdownTask(ServerPromotion::stop).schedule();
    }

    private ServerPromotion() {}

    public static boolean start(BasicServerInfo networkServer) {
        if (SENDER_SOCKET != null) return false;
        try {
            SENDER_SOCKET = new DatagramSocket(NetworkUtils.getFreePort());
        } catch (SocketException e) {
            LOGGER.warn("Could not bind to the port!", e);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        SENDER_TASK = MinecraftServer.getSchedulerManager().buildTask(() -> {
                    if (!MinecraftServer.getServer().isOpen()) return;

                    String jsonString = GSON.toJson(networkServer);
                    final byte[] data = jsonString.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, MULTICAST_ADDRESS);

                    try {
                        SENDER_SOCKET.send(packet);
                    } catch (IOException e) {
                        LOGGER.warn("Could not send network server promotion packet!", e);
                    }
                }).repeat(Duration.ofSeconds(1)).schedule();
        return true;
    }

    public static boolean stop() {
        if (SENDER_SOCKET == null) return false;

        SENDER_SOCKET.close();
        SENDER_TASK.cancel();

        SENDER_SOCKET = null;
        SENDER_TASK = null;

        return true;
    }
}
