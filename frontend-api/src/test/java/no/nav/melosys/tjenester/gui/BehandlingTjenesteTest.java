package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.EndreBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.EndreBehandlingsfristDto;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto;
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
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingTjenesteTest {
    private static final Logger log = LoggerFactory.getLogger(BehandlingTjenesteTest.class);
    private static final String TIDLIGERE_MEDLEMSPERIODER_SCHEMA = "behandlinger-tidligeremedlemsperioder-post-schema.json";
    private static final String BEHANDLINGER_SCHEMA = "behandlinger-behandling-schema.json";
    private static final String ENDRE_BEHANDLINGSTEMA_SCHEMA = "behandlinger-endrebehandlingstema-schema.json";
    private static final String ENDRE_BEHANDLINGSTEMA_POST_SCHEMA = "behandlinger-endrebehandlingstema-post-schema.json";
    private static final String ENDRE_BEHANDLINGSSTATUS_SCHEMA = "behandlinger-status-schema.json";
    private static final String ENDRE_BEHANDLINGSTYPE_SCHEMA = "behandlinger-type-schema.json";
    private static final String ENDRE_BEHANDLINGSSTATUS_POST_SCHEMA = "behandlinger-status-post-schema.json";
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
    private BehandlingsresultatService behandlingsresultatService;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        behandlingTjeneste = new BehandlingTjeneste(behandlingService, saksopplysningerTilDto, saksbehandlerService, mock(Aksesskontroll.class), behandlingsresultatService);

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
    void endreBehandling() {
        final var sakstype = Sakstyper.EU_EOS;
        final var behandlingstype = Behandlingstyper.SOEKNAD;
        final var behandlingstema = Behandlingstema.ARBEID_I_UTLANDET;
        final var behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        final var behandlingsfrist = LocalDate.now();

        var endreBehandlingDto = new EndreBehandlingDto(sakstype, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist);
        behandlingTjeneste.endreBehandling(BEHANDLING_ID, endreBehandlingDto);

        verify(behandlingService).endreBehandling(BEHANDLING_ID, sakstype, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist);
    }

    @Test
    void knyttMedlemsperioder() {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = PERIODE_IDER;

        behandlingTjeneste.knyttMedlemsperioder(BEHANDLING_ID, tidligereMedlemsperioderDto);
        verify(behandlingService).knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDER);
    }

    @Test
    void hentMedlemsperioder() {
        when(behandlingService.hentMedlemsperioder(BEHANDLING_ID)).thenReturn(PERIODE_IDER);

        ResponseEntity<TidligereMedlemsperioderDto> response = behandlingTjeneste.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(TidligereMedlemsperioderDto.class);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = response.getBody();
        assertThat(tidligereMedlemsperioderDto.periodeIder).containsAll(PERIODE_IDER);

        verify(behandlingService).hentMedlemsperioder(BEHANDLING_ID);
    }

    @Test
    void endreBehandlingsfrist() {
        LocalDate frist = LocalDate.now().plusWeeks(1);
        EndreBehandlingsfristDto endreBehandlingsfristDto = new EndreBehandlingsfristDto(frist);

        behandlingTjeneste.endreBehandlingsfrist(BEHANDLING_ID, endreBehandlingsfristDto);
        verify(behandlingService).endreBehandlingsfrist(BEHANDLING_ID, frist);
    }
}
