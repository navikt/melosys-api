package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlRestService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MedlPeriodeServiceTest {

    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private MedlRestService medlRestService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private UtpekingsperiodeRepository utpekingsperiodeRepository;

    private MedlPeriodeService medlPeriodeService;

    private static final String FNR = "12345678901";
    private static final Long MEDL_PERIODE_ID = 99L;

    @Before
    public void setUp() throws Exception {
        medlPeriodeService = new MedlPeriodeService(persondataFasade, medlRestService, behandlingsresultatService,
            lovvalgsperiodeRepository, anmodningsperiodeRepository, utpekingsperiodeRepository);

        when(medlRestService.opprettPeriodeForeløpig(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        when(medlRestService.opprettPeriodeUnderAvklaring(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        when(medlRestService.opprettPeriodeEndelig(anyString(), any(Lovvalgsperiode.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsResultat());
        when(persondataFasade.hentFolkeregisterIdent(anyString())).thenReturn(FNR);
    }

    @Test
    public void hentPeriodeListe() throws TekniskException {
        medlPeriodeService.hentPeriodeListe(FNR, LocalDate.now(), LocalDate.now().plusMonths(2));

        verify(medlRestService).hentPeriodeListe(eq(FNR), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    public void opprettPeriodeForeløpig() throws FunksjonellException, TekniskException {
        medlPeriodeService.opprettPeriodeForeløpig(new Utpekingsperiode(), 1L, true);

        verify(medlRestService).opprettPeriodeForeløpig(eq(FNR), any(Utpekingsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(utpekingsperiodeRepository).save(any(Utpekingsperiode.class));
    }

    @Test
    public void opprettPeriodeUnderAvklaring() throws FunksjonellException, TekniskException {
        medlPeriodeService.opprettPeriodeUnderAvklaring(new Anmodningsperiode(), 1L, false);

        verify(medlRestService).opprettPeriodeUnderAvklaring(eq(FNR), any(Anmodningsperiode.class), eq(KildedokumenttypeMedl.HENV_SOKNAD));
        verify(anmodningsperiodeRepository).save(any(Anmodningsperiode.class));
    }

    @Test
    public void opprettPeriodeEndelig() throws MelosysException, TekniskException {
        medlPeriodeService.opprettPeriodeEndelig(new Lovvalgsperiode(), 1L, true);

        verify(medlRestService).opprettPeriodeEndelig(eq(FNR), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeRepository).save(any(Lovvalgsperiode.class));
    }

    @Test
    public void opprettPeriodeEndeligFtrl() throws Exception {
        medlPeriodeService.opprettPeriodeEndeligFtrl(1L, new Medlemskapsperiode());

        verify(medlRestService).opprettPeriodeEndelig(eq(FNR), any(Medlemskapsperiode.class), any(KildedokumenttypeMedl.class));
    }

    @Test
    public void oppdaterPeriodeEndelig() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false);

        verify(medlRestService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void oppdaterPeriodeForeløpig() throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, false);

        verify(medlRestService).oppdaterPeriodeForeløpig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void avvisPeriode() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID);
        verify(medlRestService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.AVVIST));
    }

    @Test
    public void avvisPeriodeFeilregistrert() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID);
        verify(medlRestService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.FEILREGISTRERT));
    }

    @Test
    public void avvisPeriodeOpphørt() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID);
        verify(medlRestService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.OPPHORT));
    }

    @Test
    public void avsluttTidligereMedlPeriode_behandlingOgPeriodeFinnes_avviserPeriode() throws FunksjonellException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(1L));
        verify(medlRestService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.AVVIST));
    }

    @Test
    public void avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() throws FunksjonellException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of());
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(1L));
        verify(medlRestService, never()).avvisPeriode(anyLong(), any(StatusaarsakMedl.class));
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
