package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.*;

public class Settings {
    private static final File SETTINGS_FILE = new File("autonetwork.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static SettingsState currentSettings;

    private Settings() { }

    public static void read() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE));
            currentSettings = GSON.fromJson(reader, SettingsState.class);
        } catch (FileNotFoundException e) {
            try {
                currentSettings = new SettingsState();
                write();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void write() throws IOException {
        String json = GSON.toJson(currentSettings);
        Writer writer = new FileWriter(SETTINGS_FILE);
        writer.write(json);
        writer.close();
    }

    // data structure

    private static class SettingsState {
        @Expose
        private final boolean PROMOTE;
        @Expose private final boolean SHOW_MENU;

        @Expose private final String GROUP;
        @Expose private final String ITEM_SNBT;

        @Expose private final String MIN_VERSION;
        @Expose private final Integer MIN_PROTOCOL;

        @Expose private final Integer MAX_PLAYERS;
        @Expose private final boolean SHOW_CURRENT_PLAYERS;

        public SettingsState() {
            this.PROMOTE = true;
            this.SHOW_MENU = false;
            this.GROUP = "NOGROUP";
            this.ITEM_SNBT = "";
            this.MIN_VERSION = null;
            this.MIN_PROTOCOL = null;
            this.MAX_PLAYERS = null;
            this.SHOW_CURRENT_PLAYERS = true;
        }
    }


    // getters

    public static boolean isPromote() { return currentSettings.PROMOTE; }
    public static boolean isShowMenu() { return currentSettings.SHOW_MENU; }

    public static String getGroup() { return currentSettings.GROUP; }
    public static String getMenuItemSNBT() { return currentSettings.ITEM_SNBT; }

    public static BasicServerInfo.Version getMinVersion() {
        if (currentSettings.MIN_VERSION == null || currentSettings.MIN_PROTOCOL == null) return null;
        return new BasicServerInfo.Version(currentSettings.MIN_VERSION, currentSettings.MIN_PROTOCOL);
    }

    public static Integer getMaxPlayers() { return currentSettings.MAX_PLAYERS; }
    public static boolean isShowCurrentPlayers() { return currentSettings.SHOW_CURRENT_PLAYERS; }
}
