package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.HashSet;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import no.nav.melosys.tjenester.gui.dto.SoeknadTilleggsDataDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class SoeknadTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(SoeknadTjenesteTest.class);

    private SoeknadTjeneste soeknadTjeneste;

    private SoeknadDokument soeknadDokument;

    @Override
    public String schemaNavn() {
        return "soknad-schema.json";
    }

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {

        SoeknadService soeknadService = mock(SoeknadService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);

        Tilgang tilgang = mock(Tilgang.class);
        soeknadTjeneste = new SoeknadTjeneste(soeknadService, registerOppslagService, tilgang);
        EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .randomize(GeografiskAdresse.class, () -> new EasyRandom().nextObject(SemistrukturertAdresse.class))
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnr").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9)));

        soeknadDokument = random.nextObject(SoeknadDokument.class);
        when(soeknadService.hentSoeknad(anyLong())).thenReturn(soeknadDokument);

        OrganisasjonDokument organisasjonDokument = random.nextObject(OrganisasjonDokument.class);
        when(registerOppslagService.hentOrganisasjoner(anySet())).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        PersonDokument personDokument = random.nextObject(PersonDokument.class);
        when(registerOppslagService.hentPersoner(anySet())).thenReturn(new HashSet<>(Collections.singletonList(personDokument)));
    }

    @Test
    public void testTilleggsDataDto() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        SoeknadTilleggsDataDto tilleggDataDto = soeknadTjeneste.hentTilleggsData(soeknadDokument);

        assertThat(tilleggDataDto.organisasjoner.size()).isEqualTo(1);
        assertThat(tilleggDataDto.personer.size()).isEqualTo(1);
    }

    @Test
    public void testHentSøknad() throws Exception {
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
    public void soeknadDokumentSchemaValidering() throws Exception {
        Response response = soeknadTjeneste.hentSøknad(1222L);
        SoeknadDto søknadDto = (SoeknadDto) response.getEntity();

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

    @Test(expected = FunksjonellException.class)
    public void lagreSoeknad_ikkeRedigerbarBehandling_girFeil() throws FunksjonellException, TekniskException {
        SoeknadService soeknadService = mock(SoeknadService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        Tilgang tilgang = mock(Tilgang.class);
        SoeknadTjeneste soeknadTjeneste = new SoeknadTjeneste(soeknadService, registerOppslagService, tilgang);

        doThrow(FunksjonellException.class).when(tilgang).sjekkRedigerbar(anyLong());

        soeknadTjeneste.registrerSøknad(new SoeknadDto(1L, soeknadDokument));
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void hentSoeknad_ikkeTilgang_girFeil() throws FunksjonellException, TekniskException {
        SoeknadService soeknadService = mock(SoeknadService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        Tilgang tilgang = mock(Tilgang.class);
        SoeknadTjeneste soeknadTjeneste = new SoeknadTjeneste(soeknadService, registerOppslagService, tilgang);

        doThrow(SikkerhetsbegrensningException.class).when(tilgang).sjekk(anyLong());
        soeknadTjeneste.hentSøknad(1);
    }
}