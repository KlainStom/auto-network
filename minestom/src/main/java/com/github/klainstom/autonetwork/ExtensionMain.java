package com.github.klainstom.autonetwork;

import net.minestom.server.MinecraftServer;
import net.minestom.server.extensions.Extension;
import net.minestom.server.item.ItemStack;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.nbt.SNBTParser;

import java.io.StringReader;

public class ExtensionMain extends Extension {
    @Override
    public void initialize() {
        MinecraftServer.LOGGER.info("$name$ initialize.");
        Settings.read();
        BasicServerInfo info = null;
        if (Settings.isPromote()) {
            BasicServerInfo.Address address = new BasicServerInfo.Address(MinecraftServer.getServer().getAddress(),
                    MinecraftServer.getServer().getPort());
            BasicServerInfo.Version version = new BasicServerInfo.Version(MinecraftServer.VERSION_NAME,
                    MinecraftServer.PROTOCOL_VERSION);
            BasicServerInfo.Players players = new BasicServerInfo.Players(Settings.getMaxPlayers(),
                    Settings.isShowCurrentPlayers() ?
                            MinecraftServer.getConnectionManager().getOnlinePlayers().size() : -1);
            ItemStack menuItem = null;
            try {
                menuItem = ItemStack.fromItemNBT((NBTCompound) new SNBTParser(
                        new StringReader(Settings.getMenuItemSNBT())).parse());
            } catch (NBTException | NullPointerException e) {
                MinecraftServer.LOGGER.error("Could not load item SNBT", e);
            }

            if (menuItem == null)
                info = new BasicServerInfo(Settings.getGroup(), address,
                        version, Settings.getMinVersion(), players);
            else info = new MenuServerInfo(Settings.getGroup(),
                    address, version, Settings.getMinVersion(),
                    players, new MenuServerInfo.Representation(menuItem));
            ServerPromotion.start(info);
        }
        if (Settings.isShowMenu()) {
            if (info != null) ServerMenu.setOwnId(info.getId());
            ServerMenu.activate();
        }
    }

    @Override
    public void terminate() {
        MinecraftServer.LOGGER.info("$name$ terminate.");

    }
}
