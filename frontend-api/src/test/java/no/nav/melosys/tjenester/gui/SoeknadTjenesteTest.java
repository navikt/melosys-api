package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class SoeknadTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(SoeknadTjenesteTest.class);


    private SoeknadTjeneste soeknadTjeneste;

    @Override
    public String schemaNavn() {
        return "soknad-schema.json";
    }

    @Before
    public void setUp() throws IkkeFunnetException {

        SoeknadService soeknadService = mock(SoeknadService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);

        Tilgang tilgang = mock(Tilgang.class);
        soeknadTjeneste = new SoeknadTjeneste(soeknadService, null, registerOppslagService, tilgang);

        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .overrideDefaultInitialization(true).collectionSizeRange(1, 4).build();

        SoeknadDokument soeknadDokument = random.nextObject(SoeknadDokument.class);
        when(soeknadService.hentSoeknad(anyLong())).thenReturn(soeknadDokument);

    }

    @Test
    public void soeknadDokumentSchemaValidering() throws IOException, JSONException {

        Response response = soeknadTjeneste.hentSøknad(1222L);
        SoeknadDto søknadDto = (SoeknadDto)response.getEntity();

        assertThat(søknadDto).isNotNull();

        ObjectMapper mapper = objectMapper();
        String jsonInString = mapper.writeValueAsString(søknadDto);

        try {
            Schema schema = hentSchema();
            schema.validate(new JSONObject(jsonInString));

        } catch (ValidationException e) {
            log.error("Feil ved validering schema for Søknad dokument");
            e.getCausingExceptions().stream()
                .map(ValidationException::toJSON)
                .forEach(jsonObject -> log.error(jsonObject.toString()));
            throw e;
        }
    }
}

