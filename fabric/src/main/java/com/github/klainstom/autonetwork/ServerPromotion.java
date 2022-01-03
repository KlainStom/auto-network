package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ServerPromotion {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final InetSocketAddress MULTICAST_ADDRESS =
            new InetSocketAddress("224.0.2.60", 4446);
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPromotion.class);

    private static volatile DatagramSocket SENDER_SOCKET = null;
    private static volatile RepeatingTask SENDER_TASK = null;

    private ServerPromotion() {}

    public static void start(BasicServerInfo networkServer) {
        if (SENDER_SOCKET != null) return;
        try {
            SENDER_SOCKET = new DatagramSocket(0);
        } catch (SocketException e) {
            LOGGER.warn("Could not bind to the port!", e);
            return;
        }

        SENDER_TASK = new RepeatingTask(1, () -> {
                    String jsonString = GSON.toJson(networkServer);
                    final byte[] data = jsonString.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(data, data.length, MULTICAST_ADDRESS);

                    try {
                        SENDER_SOCKET.send(packet);
                    } catch (IOException e) {
                        LOGGER.warn("Could not send network server promotion packet!", e);
                    }
                });

        SENDER_TASK.schedule();
    }

    public static void stop() {
        if (SENDER_SOCKET == null) return;

        SENDER_SOCKET.close();
        SENDER_TASK.cancel();

        SENDER_SOCKET = null;
        SENDER_TASK = null;
    }
}
