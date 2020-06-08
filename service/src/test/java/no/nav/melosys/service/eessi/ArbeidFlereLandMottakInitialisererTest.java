package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArbeidFlereLandMottakInitialisererTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;

    private ArbeidFlereLandMottakInitialiserer arbeidFlereLandMottakInitialiserer;

    private final long behandlingID = 123;
    private final Long gsakSaksnummer = 1111L;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;
    private Fagsak fagsak;
    private MelosysEessiMelding melosysEessiMelding;
    private Prosessinstans prosessinstans;

    @Before
    public void setup() throws IkkeFunnetException {
        arbeidFlereLandMottakInitialiserer = new ArbeidFlereLandMottakInitialiserer(fagsakService, behandlingService, behandlingsresultatService, oppgaveService);

        behandling = new Behandling();
        behandling.setId(behandlingID);
        fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));

        melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setBucType(BucType.LA_BUC_02.name());
        melosysEessiMelding.setSedType(SedType.A003.name());
        prosessinstans = new Prosessinstans();

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);

        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
    }

    @Test
    public void finnsakOgBestemRuting_utenArkivsaknummer_forventNySakRuting() throws FunksjonellException, TekniskException {
        RutingResultat rutingResultat = arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), null);
        assertThat(rutingResultat).isEqualTo(RutingResultat.NY_SAK);
    }

    @Test
    public void finnsakOgBestemRuting_fagsakEksistererIkke_forventNySakRuting() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(new Prosessinstans(), 0L))
            .withMessageContaining("Finner ingen sak tilknyttet");

    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpekt_forventNyBehandling() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.NY_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaNorgeUtpekt_forventNyBehandling() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.NY_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv_forventIngenBehandlingOpprettOppgave() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any());
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv_forventIngenBehandlingStatusVurderDokument() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.INGEN_BEHANDLING);
        verify(behandlingService).oppdaterStatus(eq(behandlingID), eq(Behandlingsstatus.VURDER_DOKUMENT));
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode_forventIngenBehandling() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        melosysEessiMelding.setPeriode(new Periode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom()));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.INGEN_BEHANDLING);
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode_forventNyBehandling() throws FunksjonellException, TekniskException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        melosysEessiMelding.setPeriode(new Periode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom().plusDays(1)));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThat(arbeidFlereLandMottakInitialiserer.finnSakOgBestemRuting(prosessinstans, gsakSaksnummer)).isEqualTo(RutingResultat.NY_BEHANDLING);
    }
}