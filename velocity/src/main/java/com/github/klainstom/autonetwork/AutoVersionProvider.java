package com.github.klainstom.autonetwork;

import com.velocitypowered.api.proxy.ServerConnection;
import com.viaversion.viaversion.VelocityPlugin;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.base.BaseVersionProvider;
import com.viaversion.viaversion.velocity.platform.VelocityViaInjector;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.stream.IntStream;

// TODO: 16.11.21 Clean up this class
public class AutoVersionProvider extends BaseVersionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoVersionProvider.class);
    private static Method getAssociation;

    static {
        try {
            getAssociation = Class.forName("com.velocitypowered.proxy.connection.MinecraftConnection").getMethod("getAssociation");
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getClosestServerProtocol(UserConnection connection) throws Exception {
        return connection.isClientSide() ? getBackProtocol(connection) : getFrontProtocol(connection);
    }

    private int getBackProtocol(UserConnection user) throws Exception {
        ChannelHandler mcHandler = user.getChannel().pipeline().get("handler");
        InetSocketAddress address = ((ServerConnection) getAssociation.invoke(mcHandler)).getServerInfo().getAddress();
        BasicServerInfo networkServer = ServerDiscovery.getNetworkServers().get(address);
        int protocol;
        if (networkServer != null) {
            protocol = networkServer.getVersion().getProtocol();
        } else {
            protocol = -1;
        }
        LOGGER.info(String.format("Server %s uses protocol %d", address, protocol));
        return protocol;
    }

    private int getFrontProtocol(UserConnection user) throws Exception {
        int playerVersion = user.getProtocolInfo().getProtocolVersion();

        IntStream versions = com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS.stream()
                .mapToInt(com.velocitypowered.api.network.ProtocolVersion::getProtocol);

        // Modern forwarding mode needs 1.13 Login plugin message
        if (VelocityViaInjector.getPlayerInfoForwardingMode != null
                && ((Enum<?>) VelocityViaInjector.getPlayerInfoForwardingMode.invoke(VelocityPlugin.PROXY.getConfiguration()))
                .name().equals("MODERN")) {
            versions = versions.filter(ver -> ver >= ProtocolVersion.v1_13.getVersion());
        }
        int[] compatibleProtocols = versions.toArray();

        // Bungee supports it
        if (Arrays.binarySearch(compatibleProtocols, playerVersion) >= 0)
            return playerVersion;

        // Older than bungee supports, get the lowest version
        if (playerVersion < compatibleProtocols[0]) {
            return compatibleProtocols[0];
        }

        // Loop through all protocols to get the closest protocol id that bungee supports (and that viaversion does too)

        // This is more of a workaround for snapshot support by bungee.
        for (int i = compatibleProtocols.length - 1; i >= 0; i--) {
            int protocol = compatibleProtocols[i];
            if (playerVersion > protocol && ProtocolVersion.isRegistered(protocol))
                return protocol;
        }

        Via.getPlatform().getLogger().severe("Panic, no protocol id found for " + playerVersion);
        return playerVersion;
    }
}
