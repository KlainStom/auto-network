package com.github.klainstom.autonetwork;

import com.google.common.collect.Sets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.PluginMessagePacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerMenu {
    private static final Set<Instance> ACTIVE_INSTANCES = new HashSet<>();
    private static final EventNode<PlayerEvent> PLAYER_NODE = EventNode.type("players", EventFilter.PLAYER,
            ((playerEvent, player) -> ACTIVE_INSTANCES.contains(player.getInstance())));

    private static final Component MENU_TITLE = Component.text("Server menu", NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false);
    private static final ItemStack MENU_ITEM = ItemStack.of(Material.COMPASS).withDisplayName(MENU_TITLE);


    private static String OWN_ID = null;
    public static void setOwnId(String serverId) {
        OWN_ID = serverId;
    }
    public static void activate() {
        MinecraftServer.getGlobalEventHandler().addChild(PLAYER_NODE);
        UPDATE_TASK.schedule();
    }
    public static void deactivate() {
        MinecraftServer.getGlobalEventHandler().removeChild(PLAYER_NODE);
        UPDATE_TASK.cancel();
    }

    public static void activateFor(Instance instance) {
        ACTIVE_INSTANCES.add(instance);
    }
    public static void deactivateFor(Instance instance) {
        ACTIVE_INSTANCES.remove(instance);
    }

    private static Task UPDATE_TASK = null;
    private static final Inventory MENU_INVENTORY = new Inventory(InventoryType.CHEST_6_ROW, MENU_TITLE);
    private static final Inventory ADMIN_MENU_INVENTORY = new Inventory(InventoryType.CHEST_6_ROW, MENU_TITLE);

    static {
        MinecraftServer.getSchedulerManager().buildShutdownTask(ServerMenu::deactivate);

        PLAYER_NODE.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.getInventory().setItemStack(4, MENU_ITEM);
            player.setHeldItemSlot((byte) 4);
        });

        PLAYER_NODE.addListener(PlayerUseItemEvent.class, event -> {
            if (!event.getItemStack().equals(MENU_ITEM)) return;
            if (event.getPlayer().hasPermission("admin.autonetwork"))
                event.getPlayer().openInventory(ADMIN_MENU_INVENTORY);
            else event.getPlayer().openInventory(MENU_INVENTORY);
        });

        PLAYER_NODE.addListener(ItemDropEvent.class, event -> event.setCancelled(true));

        PLAYER_NODE.addListener(InventoryPreClickEvent.class, event -> {
            event.setCancelled(true);
            String serverName = event.getClickedItem().getTag(Tag.String("serverId"));
            if (serverName == null) return;

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream msg = new DataOutputStream(out);
                msg.writeUTF("Connect");
                msg.writeUTF(serverName);
                event.getPlayer().getPlayerConnection().sendPacket(
                        new PluginMessagePacket("bungeecord:main", out.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        UPDATE_TASK = MinecraftServer.getSchedulerManager().buildTask(() -> {
            // === for normal players ===
            Set<ItemStack> serversInMenu = new HashSet<>(List.of(MENU_INVENTORY.getItemStacks()));
            Set<ItemStack> serversInNetwork = ServerDiscovery.getNetworkServers().values().stream()
                    .map(s -> getItemStackForNetworkServer(s, false)).collect(Collectors.toSet());
            // buttons to remove
            for (ItemStack button : Sets.difference(serversInMenu, serversInNetwork)) {
                MENU_INVENTORY.takeItemStack(button, TransactionOption.ALL);
            }
            // buttons to add
            for (ItemStack button : Sets.difference(serversInNetwork, serversInMenu)) {
                MENU_INVENTORY.addItemStack(button);
            }

            // === for admins ===
            serversInMenu = new HashSet<>(List.of(ADMIN_MENU_INVENTORY.getItemStacks()));
            serversInNetwork = ServerDiscovery.getNetworkServers().values().stream()
                    .map(s -> getItemStackForNetworkServer(s, true)).collect(Collectors.toSet());
            // buttons to remove
            for (ItemStack button : Sets.difference(serversInMenu, serversInNetwork)) {
                ADMIN_MENU_INVENTORY.takeItemStack(button, TransactionOption.ALL);
            }
            // buttons to add
            for (ItemStack button : Sets.difference(serversInNetwork, serversInMenu)) {
                ADMIN_MENU_INVENTORY.addItemStack(button);
            }
            // TODO: 09.10.21 sort the server buttons into groups and by players online
        }).repeat(Duration.ofSeconds(1)).build();
    }

    private static ItemStack getItemStackForNetworkServer(MenuServerInfo server,
                                                          boolean showAddressInfo) {
        ItemStack button = server.getRepresentation().getItem().withTag(Tag.String("serverId"), server.getId());

        Component buttonName = button.getDisplayName();
        if (buttonName == null) buttonName = Component.text("Untitled", NamedTextColor.RED);
        buttonName = buttonName.decoration(TextDecoration.ITALIC, false);
        button = button.withDisplayName(buttonName);

        List<Component> lore = new ArrayList<>(button.getLore());

        if (server.getId().equals(OWN_ID))
            lore.add(0, Component.text("Already on this server", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text("Server version: "+server.getVersion().getName(),
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (server.getMinVersion() != null)
            lore.add(Component.text("Minimum version: "+server.getMinVersion().getName(),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        if (showAddressInfo) {
            BasicServerInfo.Address address = server.getAddress();
            lore.add(Component.text("Address: "+new InetSocketAddress(address.getHost(), address.getPort()),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }

        return button.withLore(lore);
    }
}
