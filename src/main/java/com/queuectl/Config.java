package com.queuectl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private final Properties props = new Properties();
    private final Path path = Path.of("config.properties");

    public Config() {
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            props.load(in);
        } catch (IOException ignored) {
            // no config yet
        }
    }

    public String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            props.store(out, "queuectl config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
