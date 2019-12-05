package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import no.nav.melosys.tjenester.gui.dto.tildto.SaksopplysningerTilDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(BehandlingTjenesteTest.class);
    private static final String TIDLIGERE_MEDLEMSPERIODER_SCHEMA = "behandlinger-tidligeremedlemsperioder-post-schema.json";
    private static final String BEHANDLINGER_SCHEMA = "behandlinger-behandling-schema.json";

    private BehandlingTjeneste behandlingTjeneste;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private SaksopplysningerTilDto saksopplysningerTilDto;
    private EasyRandom random;

    @Before
    public void setUp() {
        behandlingTjeneste = new BehandlingTjeneste(behandlingService, saksopplysningerTilDto, mock(TilgangService.class));

        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .excludeField(named("tilleggsinformasjonDetaljer").and(ofType(TilleggsinformasjonDetaljer.class)).and(inClass(Tilleggsinformasjon.class)))
            .excludeField(named("sed").and(ofType(SedDokument.class)))
            .stringLengthRange(2, 10)
            .randomize(GeografiskAdresse.class, () -> random.nextObject(SemistrukturertAdresse.class))
            .randomize(MidlertidigPostadresse.class, () -> Math.random() > 0.5 ? random.nextObject(MidlertidigPostadresseNorge.class) : random.nextObject(MidlertidigPostadresseUtland.class))
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9))
        );
    }

    @Test
    public void behandlingerPerioderValidering() throws IOException {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = Arrays.asList(2L, 3L, 5L);
        valider(tidligereMedlemsperioderDto, TIDLIGERE_MEDLEMSPERIODER_SCHEMA, log);
    }

    @Test
    public void hentBehandling_erSchemaValidert() throws IOException {
        BehandlingDto behandlingDto = random.nextObject(BehandlingDto.class);
        behandlingDto.getSaksopplysninger().setSed(null);
        String jsonString = objectMapperMedKodeverkServiceStub().writeValueAsString(behandlingDto);
        valider(jsonString, BEHANDLINGER_SCHEMA, log);
    }

    @Test
    public void knyttMedlemsperioder() throws Exception {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L, 5L);
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = periodeIder;

        behandlingTjeneste.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioderDto);
        verify(behandlingService, times(1)).knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void hentMedlemsperioder() throws Exception {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L, 5L);
        when(behandlingService.hentMedlemsperioder(behandlingID)).thenReturn(periodeIder);

        ResponseEntity response = behandlingTjeneste.hentMedlemsperioder(behandlingID);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(TidligereMedlemsperioderDto.class);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = (TidligereMedlemsperioderDto) response.getBody();
        assertThat(tidligereMedlemsperioderDto.periodeIder).containsAll(periodeIder);

        verify(behandlingService, times(1)).hentMedlemsperioder(behandlingID);

    }
}
