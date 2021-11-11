package com.github.klainstom.autonetwork;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class LobbyCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player) {
            Player player = (Player) invocation.source();
            for (RegisteredServer registeredServer : AutoNetworkVelocity.SERVER.matchServer("lobby-")) {
                player.createConnectionRequest(registeredServer).connect();
                break;
            }
        }
    }
}
