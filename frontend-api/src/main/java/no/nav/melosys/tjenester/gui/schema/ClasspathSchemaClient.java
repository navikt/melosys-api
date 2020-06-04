package no.nav.melosys.tjenester.gui.schema;

import org.everit.json.schema.loader.SchemaClient;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ClasspathSchemaClient implements SchemaClient {
    public InputStream get(String url) {
        try {
            url = url.replace("http://melosys.nav.no/schemas", "");
            return new ClassPathResource(url).getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}