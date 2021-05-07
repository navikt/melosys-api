package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.EndreBehandlingstemaService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.EndreBehandlingsfristDto;
import no.nav.melosys.tjenester.gui.dto.EndreBehandlingstemaDto;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import no.nav.melosys.tjenester.gui.dto.tildto.SaksopplysningerTilDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(BehandlingTjenesteTest.class);
    private static final String TIDLIGERE_MEDLEMSPERIODER_SCHEMA = "behandlinger-tidligeremedlemsperioder-post-schema.json";
    private static final String BEHANDLINGER_SCHEMA = "behandlinger-behandling-schema.json";
    private static final String ENDRE_BEHANDLINGSTEMA_SCHEMA = "behandlinger-endrebehandlingstema-schema.json";
    private static final String ENDRE_BEHANDLINGSTEMA_POST_SCHEMA = "behandlinger-endrebehandlingstema-post-schema.json";
    private static final long BEHANDLING_ID = 11L;
    private static final List<Long> PERIODE_IDER = Arrays.asList(2L, 3L, 5L);

    private BehandlingTjeneste behandlingTjeneste;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private SaksopplysningerTilDto saksopplysningerTilDto;
    @Mock
    private SaksbehandlerService saksbehandlerService;
    @Mock
    private EndreBehandlingstemaService endreBehandlingstemaService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private EasyRandom random;

    @BeforeEach
    void setUp() {
        behandlingTjeneste = new BehandlingTjeneste(behandlingService, saksopplysningerTilDto, mock(TilgangService.class), saksbehandlerService, endreBehandlingstemaService, oppgaveService, applicationEventPublisher);

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
            .randomize(named("fnrAnnenForelder").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9))
        );
    }

    @Test
    void behandlingerPerioderValidering() throws Exception {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = PERIODE_IDER;
        valider(tidligereMedlemsperioderDto, TIDLIGERE_MEDLEMSPERIODER_SCHEMA, log);
    }

    @Test
    void hentBehandling_erSchemaValidert() throws Exception {
        BehandlingDto behandlingDto = random.nextObject(BehandlingDto.class);
        behandlingDto.getSaksopplysninger().setSed(null);
        String jsonString = objectMapperMedKodeverkServiceStub()
            .writerWithView(DokumentView.FrontendApi.class)
            .writeValueAsString(behandlingDto);
        valider(jsonString, BEHANDLINGER_SCHEMA, log);
    }

    @Test
    void hentMuligeBehandlinstemaValidering() throws IOException, MelosysException {
        when(endreBehandlingstemaService.hentMuligeBehandlingstema(BEHANDLING_ID)).thenReturn(BEHANDLINGSTEMA_SØKNAD);
        List<Behandlingstema> muligeBehandlingstema = behandlingTjeneste.hentEndreBehandlingstema(BEHANDLING_ID).getBody();
        validerArray(muligeBehandlingstema, ENDRE_BEHANDLINGSTEMA_SCHEMA, log);
    }

    @Test
    void endreBehandlinstemaValidering() throws Exception {
        EndreBehandlingstemaDto endreBehandlingstemaDto = new EndreBehandlingstemaDto();
        endreBehandlingstemaDto.setBehandlingstema(Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND.getKode());
        valider(endreBehandlingstemaDto, ENDRE_BEHANDLINGSTEMA_POST_SCHEMA, log);
    }

    @Test
    void knyttMedlemsperioder() throws Exception {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = PERIODE_IDER;

        behandlingTjeneste.knyttMedlemsperioder(BEHANDLING_ID, tidligereMedlemsperioderDto);
        verify(behandlingService).knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDER);
    }

    @Test
    void hentMedlemsperioder() throws Exception {
        when(behandlingService.hentMedlemsperioder(BEHANDLING_ID)).thenReturn(PERIODE_IDER);

        ResponseEntity<TidligereMedlemsperioderDto> response = behandlingTjeneste.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(TidligereMedlemsperioderDto.class);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = response.getBody();
        assertThat(tidligereMedlemsperioderDto.periodeIder).containsAll(PERIODE_IDER);

        verify(behandlingService).hentMedlemsperioder(BEHANDLING_ID);
    }

    @Test
    void endreBehandlingsfrist() throws Exception {
        LocalDate frist = LocalDate.now().plusWeeks(1);
        EndreBehandlingsfristDto endreBehandlingsfristDto = new EndreBehandlingsfristDto();
        endreBehandlingsfristDto.setBehandlingsfrist(frist);

        behandlingTjeneste.endreBehandlingsfrist(BEHANDLING_ID, endreBehandlingsfristDto);
        verify(behandlingService).endreBehandlingsfrist(BEHANDLING_ID, frist);
    }
}
