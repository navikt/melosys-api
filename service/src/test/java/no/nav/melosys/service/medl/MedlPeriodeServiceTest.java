package no.nav.melosys.service.medl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.dok.melosysbrev._000072.Fag;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlRestService;
import no.nav.melosys.integrasjon.medl.MedlSoapService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MedlPeriodeServiceTest {
    private final FakeUnleash fakeUnleash = new FakeUnleash();
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private MedlSoapService medlSoapService;
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
        medlPeriodeService = new MedlPeriodeService(fakeUnleash, persondataFasade, medlSoapService, medlRestService,
            behandlingsresultatService, lovvalgsperiodeRepository, anmodningsperiodeRepository, utpekingsperiodeRepository);

        when(medlSoapService.opprettPeriodeForeløpig(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        when(medlSoapService.opprettPeriodeUnderAvklaring(anyString(), any(PeriodeOmLovvalg.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);
        when(medlSoapService.opprettPeriodeEndelig(anyString(), any(Lovvalgsperiode.class), any(KildedokumenttypeMedl.class)))
            .thenReturn(MEDL_PERIODE_ID);

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsResultat());
        when(persondataFasade.hentIdentForAktørId(anyString())).thenReturn(FNR);
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

    @Test
    public void hentPeriodeListe() throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.hentPeriodeListe(FNR, LocalDate.now(), LocalDate.now().plusMonths(2));

        verify(medlSoapService).hentPeriodeListe(eq(FNR), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    public void opprettPeriodeForeløpig() throws FunksjonellException, TekniskException {
        medlPeriodeService.opprettPeriodeForeløpig(new Utpekingsperiode(), 1L, true);

        verify(medlSoapService).opprettPeriodeForeløpig(eq(FNR), any(Utpekingsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(utpekingsperiodeRepository).save(any(Utpekingsperiode.class));
    }

    @Test
    public void opprettPeriodeUnderAvklaring() throws FunksjonellException, TekniskException {
        medlPeriodeService.opprettPeriodeUnderAvklaring(new Anmodningsperiode(), 1L, false);

        verify(medlSoapService).opprettPeriodeUnderAvklaring(eq(FNR), any(Anmodningsperiode.class), eq(KildedokumenttypeMedl.HENV_SOKNAD));
        verify(anmodningsperiodeRepository).save(any(Anmodningsperiode.class));
    }

    @Test
    public void opprettPeriodeEndelig() throws FunksjonellException, TekniskException {
        medlPeriodeService.opprettPeriodeEndelig(new Lovvalgsperiode(), 1L, true);

        verify(medlSoapService).opprettPeriodeEndelig(eq(FNR), any(Lovvalgsperiode.class), eq(KildedokumenttypeMedl.SED));
        verify(lovvalgsperiodeRepository).save(any(Lovvalgsperiode.class));
    }

    @Test
    public void oppdaterPeriodeEndelig() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false);

        verify(medlSoapService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void oppdaterPeriodeForeløpig() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(MEDL_PERIODE_ID);
        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, false);

        verify(medlSoapService).oppdaterPeriodeForeløpig(eq(lovvalgsperiode), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void avvisPeriode() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID);
        verify(medlSoapService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.AVVIST));
    }

    @Test
    public void avvisPeriodeFeilregistrert() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID);
        verify(medlSoapService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.FEILREGISTRERT));
    }

    @Test
    public void avvisPeriodeOpphørt() throws SikkerhetsbegrensningException, IkkeFunnetException {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID);
        verify(medlSoapService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.OPPHORT));
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
        verify(medlSoapService).avvisPeriode(eq(MEDL_PERIODE_ID), eq(StatusaarsakMedl.AVVIST));
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
        verify(medlSoapService, never()).avvisPeriode(anyLong(), any(StatusaarsakMedl.class));
    }
}