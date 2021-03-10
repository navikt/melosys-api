package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevmottakerMapperTest {

    @Mock
    private BrevmottakerService mockBrevmottakerService;

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private MedlemAvFolketrygdenRepository mockMedlemAvFolketrygdenRepository;

    private BrevmottakerMapper brevmottakerMapper;

    @BeforeEach
    void init() {
        brevmottakerMapper = new BrevmottakerMapper(mockBrevmottakerService, mockBehandlingService,
            mockMedlemAvFolketrygdenRepository);
    }

    @Test
    void gittMalIkkeRegistret_skalKasteFeil() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> brevmottakerMapper.finnBrevMottaker(ATTEST_A1, 123))
            .withMessage("Mangler mapping av mottakere for ATTEST_A1");
    }

    @Test
    @DisplayName("Forvaltningsmelding skal ha BRUKER som hovedmottaker, uten kopier")
    void gittForvaltningsmelding_skalHovedmottakerVæreBruker() throws Exception {
        assertThat(brevmottakerMapper.finnBrevMottaker(MELDING_FORVENTET_SAKSBEHANDLINGSTID, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                emptyList()
            );

        verifyNoInteractions(mockBehandlingService);
        verifyNoInteractions(mockBrevmottakerService);
        verifyNoInteractions(mockMedlemAvFolketrygdenRepository);
    }

    @Test
    @DisplayName("Mangelbrev til bruker skal ha BRUKER som hovedmottaker, uten kopier")
    void gittMangelbrevBruker_skalHovedmottakerVæreBruker() throws Exception {
        assertThat(brevmottakerMapper.finnBrevMottaker(MANGELBREV_BRUKER, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                emptyList()
            );

        verifyNoInteractions(mockBehandlingService);
        verifyNoInteractions(mockBrevmottakerService);
        verifyNoInteractions(mockMedlemAvFolketrygdenRepository);
    }

    @Test
    @DisplayName("Mangelbrev til arbeidsgiver skal ha ARBEIDSGIVER som hovedmottaker, med kopi til BRUKER")
    void gittMangelbrevArbeidsgiver_skalHovedmottakerVæreArbeidsgiverMedKopi() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Sakstyper.FTRL));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any())).thenReturn(lagMottakerListe(ARBEIDSGIVER));

        assertThat(brevmottakerMapper.finnBrevMottaker(MANGELBREV_ARBEIDSGIVER, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                ARBEIDSGIVER,
                List.of(BRUKER),
                emptyList()
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(MANGELBREV_ARBEIDSGIVER), eq(Mottaker.av(ARBEIDSGIVER)), any());
        verifyNoInteractions(mockMedlemAvFolketrygdenRepository);
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForVedtaksbrev(BRUKER, ARBEIDSGIVER, 10000);

        assertThat(brevmottakerMapper.finnBrevMottaker(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(ARBEIDSGIVER),
                List.of(SKATT)
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(INNVILGELSE_FOLKETRYGDLOVEN_2_8), eq(Mottaker.av(BRUKER)), any());
        verify(mockMedlemAvFolketrygdenRepository).findByBehandlingsresultatId(123);
    }

    @Test
    void gittVedtakFtrl2_8UtenFullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForVedtaksbrev(BRUKER, BRUKER, 10000);

        assertThat(brevmottakerMapper.finnBrevMottaker(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                emptyList(),
                List.of(SKATT)
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(INNVILGELSE_FOLKETRYGDLOVEN_2_8), eq(Mottaker.av(BRUKER)), any());
        verify(mockMedlemAvFolketrygdenRepository).findByBehandlingsresultatId(123);
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForVedtaksbrev(REPRESENTANT, ARBEIDSGIVER, 10000);

        assertThat(brevmottakerMapper.finnBrevMottaker(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER, ARBEIDSGIVER),
                List.of(SKATT)
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(INNVILGELSE_FOLKETRYGDLOVEN_2_8), eq(Mottaker.av(BRUKER)), any());
        verify(mockMedlemAvFolketrygdenRepository).findByBehandlingsresultatId(123);
    }

    @Test
    void gittVedtakFtrl2_8FullmektigSelvbetalende_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForVedtaksbrev(REPRESENTANT, BRUKER, 10000);

        assertThat(brevmottakerMapper.finnBrevMottaker(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER),
                List.of(SKATT)
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(INNVILGELSE_FOLKETRYGDLOVEN_2_8), eq(Mottaker.av(BRUKER)), any());
        verify(mockMedlemAvFolketrygdenRepository).findByBehandlingsresultatId(123);
    }

    @Test
    void gittVedtakFtrl2_8FullmektigIkkeSelvbetalendeIkkeInntekt_skalHovedmottakerVæreBrukerMedKopier() throws Exception {
        initMocksForVedtaksbrev(REPRESENTANT, ARBEIDSGIVER, 0);

        assertThat(brevmottakerMapper.finnBrevMottaker(INNVILGELSE_FOLKETRYGDLOVEN_2_8, 123))
            .isNotNull()
            .extracting(
                Mottakerliste::getHovedMottaker,
                Mottakerliste::getKopiMottakere,
                Mottakerliste::getFasteMottakere
            )
            .containsExactly(
                BRUKER,
                List.of(BRUKER, ARBEIDSGIVER),
                emptyList()
            );

        verify(mockBehandlingService).hentBehandling(123);
        verify(mockBrevmottakerService).avklarMottakere(eq(INNVILGELSE_FOLKETRYGDLOVEN_2_8), eq(Mottaker.av(BRUKER)), any());
        verify(mockMedlemAvFolketrygdenRepository).findByBehandlingsresultatId(123);
    }

    private void initMocksForVedtaksbrev(Aktoersroller avklartMottaker, Aktoersroller betaler, long norskinntekt) throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling(Sakstyper.FTRL));
        when(mockBrevmottakerService.avklarMottakere(any(), any(), any())).thenReturn(lagMottakerListe(avklartMottaker));
        when(mockMedlemAvFolketrygdenRepository.findByBehandlingsresultatId(anyLong())).thenReturn(lagMedlemAvFolketrygden(betaler, norskinntekt));
    }

    private Behandling lagBehandling(Sakstyper sakstype) {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        behandling.setFagsak(fagsak);

        return behandling;
    }

    private List<Aktoer> lagMottakerListe(Aktoersroller... aktoersroller) {
        List<Aktoer> aktorer = new ArrayList<>();
        Arrays.stream(aktoersroller).forEach(r -> {
            Aktoer aktoer = new Aktoer();
            aktoer.setRolle(r);
            aktorer.add(aktoer);
        });
        return aktorer;
    }

    private Optional<MedlemAvFolketrygden> lagMedlemAvFolketrygden(Aktoersroller betaler, long norskinntekt) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        FastsattTrygdeavgift fastsattTrygdeavgift = new FastsattTrygdeavgift();
        Aktoer betalesAv = new Aktoer();
        betalesAv.setRolle(betaler);

        fastsattTrygdeavgift.setBetalesAv(betalesAv);
        fastsattTrygdeavgift.setAvgiftspliktigNorskInntektMnd(norskinntekt);

        medlemAvFolketrygden.setFastsattTrygdeavgift(fastsattTrygdeavgift);
        return Optional.of(medlemAvFolketrygden);
    }
}