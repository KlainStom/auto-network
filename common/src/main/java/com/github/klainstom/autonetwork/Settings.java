package com.github.klainstom.autonetwork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public class Settings<T extends Settings.SettingsObject> {
    private static final File SETTINGS_FILE = new File("autonetwork.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private T currentSettings;

    public Settings(T defaultSettings) {
        currentSettings = defaultSettings;
    }

    public void read() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE));
            // FIXME: 11.11.21 ClassCastException due to differing loaders
            currentSettings = GSON.fromJson(reader, currentSettings.getClass().getGenericSuperclass());
        } catch (FileNotFoundException e) {
            try {
                write();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void write() throws IOException {
        String json = GSON.toJson(currentSettings);
        Writer writer = new FileWriter(SETTINGS_FILE);
        writer.write(json);
        writer.close();
    }

    public T current() {
        return currentSettings;
    }

    public interface SettingsObject {}
}
