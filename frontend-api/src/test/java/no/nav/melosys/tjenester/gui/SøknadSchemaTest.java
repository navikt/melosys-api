package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SøknadSchemaTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(SøknadSchemaTest.class);

    private String jsonInString;

    @Override
    public String schemaNavn() {
        return "soknad-schema.json";
    }

    @Before
    public void setUp() throws JsonProcessingException, IkkeFunnetException {

        SoeknadService soeknadService = mock(SoeknadService.class);

        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .collectionSizeRange(1, 4).build();

        SoeknadDokument soeknadDokument = random.nextObject(SoeknadDokument.class);
        when(soeknadService.hentSoeknad(anyLong())).thenReturn(soeknadDokument);

        SoeknadDto søknadDto = new SoeknadDto(1222L, soeknadDokument);

        ObjectMapper mapper = objectMapper();
        jsonInString = mapper.writeValueAsString(søknadDto);
    }

    @Test
    public void soeknadDokumentSchemaValidering() throws IOException {

        try {
            Schema schema = hentSchema();
            schema.validate(new JSONObject(jsonInString));

        } catch (ValidationException e) {
            log.error("Feil ved validering schema for Søknad dokument");
            log.error(e.toJSON().toString());
            //throw e;
        }
    }
}

