package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import no.nav.melosys.tjenester.gui.dto.SoeknadTilleggsDataDto;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class SoeknadTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(SoeknadTjenesteTest.class);

    private SoeknadTjeneste soeknadTjeneste;

    private SoeknadDokument soeknadDokument;

    @Mock
    private RegisterOppslagService registerOppslagService;

    @Override
    public String schemaNavn() {
        return "soknad-schema.json";
    }

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {

        SoeknadService soeknadService = mock(SoeknadService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);

        Tilgang tilgang = mock(Tilgang.class);
        soeknadTjeneste = new SoeknadTjeneste(soeknadService, null, registerOppslagService, tilgang);

        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .overrideDefaultInitialization(true)
                .collectionSizeRange(1, 4)
                .randomize(GeografiskAdresse.class, (Randomizer<GeografiskAdresse>) () -> EnhancedRandom.random(SemistrukturertAdresse.class))
                .build();

        soeknadDokument = random.nextObject(SoeknadDokument.class);
        when(soeknadService.hentSoeknad(anyLong())).thenReturn(soeknadDokument);

        OrganisasjonDokument organisasjonDokument = random.nextObject(OrganisasjonDokument.class);
        when(registerOppslagService.hentOrganisasjoner(anySet())).thenReturn(new HashSet<>(Arrays.asList(organisasjonDokument)));

        PersonDokument personDokument = random.nextObject(PersonDokument.class);
        when(registerOppslagService.hentPersoner(anySet())).thenReturn(new HashSet<>(Arrays.asList(personDokument)));
    }

    @Test
    public void testTilleggsDataDto() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        SoeknadTilleggsDataDto tilleggDataDto = soeknadTjeneste.hentTilleggsData(soeknadDokument);

        assertThat(tilleggDataDto.organisasjoner.size()).isEqualTo(1);
        assertThat(tilleggDataDto.personer.size()).isEqualTo(1);
    }

    @Test
    public void testHentSøknad() {
        Response resultat = soeknadTjeneste.hentSøknad(1L);
        assertThat(resultat.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        SoeknadDto dto = (SoeknadDto) resultat.getEntity();

        assertThat(dto.getSoeknadDokument()).isNotNull();
        SoeknadTilleggsDataDto tilleggsDto = dto.getTilleggsData();
        assertThat(tilleggsDto).isNotNull();
        assertThat(tilleggsDto.organisasjoner.size()).isEqualTo(1);
        assertThat(tilleggsDto.personer.size()).isEqualTo(1);
    }

    @Test
    public void soeknadDokumentSchemaValidering() throws IOException, JSONException, IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Response response = soeknadTjeneste.hentSøknad(1222L);
        SoeknadDto søknadDto = (SoeknadDto)response.getEntity();

        assertThat(søknadDto).isNotNull();

        ObjectMapper mapper = objectMapperMedKodeverkServiceStub();
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

