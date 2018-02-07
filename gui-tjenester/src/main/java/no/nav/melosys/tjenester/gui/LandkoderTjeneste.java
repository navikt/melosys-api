package no.nav.melosys.tjenester.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"landkoder"})
@Path("/landkoder")
@Service
@Scope(value= WebApplicationContext.SCOPE_APPLICATION)
public class LandkoderTjeneste extends RestTjeneste {

    private String jsonLandkoder;

    @Autowired
    public LandkoderTjeneste(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:landkoder.json");

        InputStream inputStream = resource.getInputStream();

        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }

        jsonLandkoder = stringBuilder.toString();
    }

    @GET
    public String getLandkoder() {
        return jsonLandkoder;
    }
}
