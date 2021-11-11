package com.github.klainstom.autonetwork;

public class BackendSettingsState implements Settings.SettingsObject{
    private final boolean PROMOTE;
    private final boolean SHOW_MENU;

    private final String GROUP;
    private final String ITEM_SNBT;

    private final String MIN_VERSION;
    private final Integer MIN_PROTOCOL;

    private final Integer MAX_PLAYERS;
    private final boolean SHOW_CURRENT_PLAYERS;

    public BackendSettingsState() {
        this.PROMOTE = true;
        this.SHOW_MENU = false;
        this.GROUP = "NOGROUP";
        this.ITEM_SNBT = null;
        this.MIN_VERSION = null;
        this.MIN_PROTOCOL = null;
        this.MAX_PLAYERS = null;
        this.SHOW_CURRENT_PLAYERS = true;
    }

    public boolean isPromote() {
        return PROMOTE;
    }

    public boolean isShowMenu() {
        return SHOW_MENU;
    }

    public String getGroup() {
        return GROUP;
    }

    public String getMenuItemSNBT() {
        return ITEM_SNBT;
    }

    public BasicServerInfo.Version getMinVersion() {
        if (this.MIN_VERSION == null || this.MIN_PROTOCOL == null) return null;
        return new BasicServerInfo.Version(this.MIN_VERSION, this.MIN_PROTOCOL);
    }

    public Integer getMaxPlayers() {
        return this.MAX_PLAYERS;
    }
    public boolean isShowCurrentPlayers() { return this.SHOW_CURRENT_PLAYERS; }
}
