package no.nav.melosys;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static java.nio.file.Paths.get;

public class ApplicationLocal {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "local");

        Path latestPath = get(System.getenv("MELOSYS_CONFIG_LOCATION")).resolve("melosys-app.properties");

        try (InputStream inputStream = Files.newInputStream(latestPath, StandardOpenOption.READ)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.forEach((key, value) -> {
                System.setProperty(key.toString(), value.toString());
            });
        } catch (IOException e) {
            throw new RuntimeException("Finner ikke config", e);
        }

        Application.main(args);
    }

}