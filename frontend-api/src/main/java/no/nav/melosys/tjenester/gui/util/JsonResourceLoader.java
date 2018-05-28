package no.nav.melosys.tjenester.gui.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JsonResourceLoader {
    public static String load(ResourceLoader resourceLoader, String filename) throws IOException {

        Resource resource = resourceLoader.getResource("classpath:" + filename);

        InputStream inputStream = resource.getInputStream();

        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        return stringBuilder.toString();
    }
}
