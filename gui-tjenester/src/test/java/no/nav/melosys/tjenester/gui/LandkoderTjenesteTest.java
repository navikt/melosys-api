package no.nav.melosys.tjenester.gui;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
public class LandkoderTjenesteTest {

    @Autowired
    ResourceLoader resourceLoader;
    private String jsonLandkoder;

    @Before
    public void setUp() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:landkoder.json");
        jsonLandkoder = new String(Files.readAllBytes(resource.getFile().toPath()));
    }

    @Test
    public void getLandkoder() {
        assertNotNull(jsonLandkoder);
        assertNotEquals("", jsonLandkoder);
    }
}
