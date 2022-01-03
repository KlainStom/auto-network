package com.github.klainstom.autonetwork;

import com.mojang.bridge.game.GameVersion;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.MinecraftVersion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.SERVER)
public class AutoNetworkFabric implements DedicatedServerModInitializer {
    private BasicServerInfo serverInfo;

    @Override
    public void onInitializeServer() {
        Settings.read();
        GameVersion version = MinecraftVersion.GAME_VERSION;
        BasicServerInfo.Address address = new BasicServerInfo.Address("", 0); // TODO: 03.01.22 get port
        ItemStack representation = null;
        try {
            representation = ItemStack.fromNbt(new StringNbtReader(
                    new com.mojang.brigadier.StringReader(Settings.getMenuItemSNBT())).parseCompound());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        if (representation == null) {
            serverInfo = new BasicServerInfo(
                    Settings.getGroup(),
                    address,
                    new BasicServerInfo.Version(version.getName(), version.getProtocolVersion()),
                    Settings.getMinVersion(),
                    new BasicServerInfo.Players(Settings.getMaxPlayers(), -1)
            );
        } else {
            serverInfo = new MenuServerInfo(
                    Settings.getGroup(),
                    address,
                    new BasicServerInfo.Version(version.getName(), version.getProtocolVersion()),
                    Settings.getMinVersion(),
                    new BasicServerInfo.Players(Settings.getMaxPlayers(), -1),
                    new MenuServerInfo.Representation(representation)
            );
        }

        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            ServerPromotion.start(serverInfo);
        }));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> {
            ServerPromotion.stop();
        }));
    }
}
