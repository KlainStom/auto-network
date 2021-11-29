package com.github.klainstom.autonetwork;

import com.google.gson.annotations.Expose;
import net.minestom.server.item.ItemStack;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.nbt.SNBTParser;

import java.io.StringReader;

public class MenuServerInfo extends BasicServerInfo {
    @Expose private final Representation representation;

    public MenuServerInfo(String name, Address address, Version version, Version minVersion, Players players, Representation representation) {
        super(name, address, version, minVersion, players);
        this.representation = representation;
    }

    public Representation getRepresentation() {
        return representation;
    }

    public static class Representation {
        @Expose private final String item;

        public Representation(ItemStack item) {
            this.item = item.toItemNBT().toSNBT();
        }

        public ItemStack getItem() {
            try {
                return ItemStack.fromItemNBT((NBTCompound) new SNBTParser(new StringReader(this.item)).parse());            } catch (NBTException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
