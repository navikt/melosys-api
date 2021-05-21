package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedlPeriodeServiceTest {

    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private MedlService medlService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private UtpekingsperiodeRepository utpekingsperiodeRepository;
    @Mock
    private MedlemskapsperiodeRepository medlemskapsperiodeRepository;

    private MedlPeriodeService medlPeriodeService;

    private static final String FNR = "12345678901";
    private static final Long MEDL_PERIODE_ID = 99L;

    @BeforeEach
    public void setUp() throws Exception {
        medlPeriodeService = new MedlPeriodeService(persondataFasade, medlService, behandlingsresultatService,
            lovvalgsperiodeRepository, anmodningsperiodeRepository, utpekingsperiodeRepository, medlemskapsperiodeRepository);
    }

    @Test
    void hentPeriodeListe() {
        medlPeriodeService.hentPeriodeListe(FNR, LocalDate.now(), LocalDate.now().plusMonths(2));

        verify(medlService).hentPeriodeListe(eq(FNR), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void opprettPeriodeForeløpig() {
        setupPeriodeForeløpig();

        medlPeriodeService.opprettPeriodeForeløpig(new Utpekingsperiode(), 1L, true);

        verify(medlService).opprettPeriodeForeløpig(eq(FNR), any(Utpekingsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(utpekingsperiodeRepository).save(any(Utpekingsperiode.class));
    }

    @Test
    void opprettPeriodeUnderAvklaring() {
        setupPeriodeUnderAvklaring();

        medlPeriodeService.opprettPeriodeUnderAvklaring(new Anmodningsperiode(), 1L, false);

        verify(medlService).opprettPeriodeUnderAvklaring(eq(FNR), any(Anmodningsperiode.class), eq(KildedokumenttypeMedl.HENV_SOKNAD));
        verify(anmodningsperiodeRepository).save(any(Anmodningsperiode.class));
    }

    @Test
    void opprettPeriodeEndelig() {
        setupPeriodeEndelig();

        medlPeriodeService.opprettPeriodeEndelig(new Lovvalgsperiode(), 1L, true);

        verify(medlService).opprettPeriodeEndelig(eq(FNR), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeRepository).save(any(Lovvalgsperiode.class));
    }

    @Test
    void opprettPeriodeEndeligFtrl_feiler() throws Exception {
        setupHappyPathBehandling();
        when(medlService.opprettPeriodeEndelig(eq(FNR), any(Medlemskapsperiode.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> medlPeriodeService.opprettPeriodeEndelig(1L, new Medlemskapsperiode()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID");

        verifyNoInteractions(medlemskapsperiodeRepository);
    }

    @Test
    void opprettPeriodeEndeligFtrl() throws Exception {
        setupHappyPathBehandling();

        medlPeriodeService.opprettPeriodeEndelig(1L, new Medlemskapsperiode());

        verify(medlService).opprettPeriodeEndelig(eq(FNR), any(Medlemskapsperiode.class), any(KildedokumenttypeMedl.class));
        verify(medlemskapsperiodeRepository).save(any(Medlemskapsperiode.class));
    }

    @Test
    void oppdaterPeriodeEndelig() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false);

        verify(medlService).oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
    }

    @Test
    void oppdaterPeriodeForeløpig() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, false);

        verify(medlService).oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD);
    }

    @Test
    void avvisPeriode() {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID);
        verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST);
    }

    @Test
    void avvisPeriodeFeilregistrert() {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID);
        verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.FEILREGISTRERT);
    }

    @Test
    void avvisPeriodeOpphørt() {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID);
        verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.OPPHORT);
    }

    @Test
    void avsluttTidligereMedlPeriode_behandlingOgPeriodeFinnes_avviserPeriode() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak);

        verify(behandlingsresultatService).hentBehandlingsresultat(1L);
        verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST);
    }

    @Test
    void avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of());
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak);

        verify(behandlingsresultatService).hentBehandlingsresultat(1L);
        verify(medlService, never()).avvisPeriode(anyLong(), any(StatusaarsakMedl.class));
    }

    private void setupPeriodeForeløpig() {
        when(medlService.opprettPeriodeForeløpig(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        setupHappyPathBehandling();
    }

    private void setupPeriodeUnderAvklaring() {
        when(medlService.opprettPeriodeUnderAvklaring(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        setupHappyPathBehandling();
    }

    private void setupPeriodeEndelig() {
        when(medlService.opprettPeriodeEndelig(anyString(), any(Lovvalgsperiode.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        setupHappyPathBehandling();
    }

    private void setupHappyPathBehandling() {
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsResultat());
        when(persondataFasade.hentFolkeregisterIdent(anyString())).thenReturn(FNR);
    }

    private Behandlingsresultat lagBehandlingsResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();

        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("456");
        fagsak.getAktører().add(aktoer);

        behandling.setFagsak(fagsak);
        behandlingsresultat.setBehandling(behandling);
        return behandlingsresultat;
    }
}
