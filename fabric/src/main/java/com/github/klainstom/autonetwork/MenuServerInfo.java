package com.github.klainstom.autonetwork;

import com.google.gson.annotations.Expose;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;

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
            this.item = item.getOrCreateNbt().toString();
        }

        public ItemStack getItem() {
            try {
                return ItemStack.fromNbt(
                        new StringNbtReader(new com.mojang.brigadier.StringReader(this.item)).parseCompound());
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
