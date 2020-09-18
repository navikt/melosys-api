package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
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
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.ArbeidFlereLandSedRuter;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArbeidFlereLandSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;

    private ArbeidFlereLandSedRuter arbeidFlereLandSedRuter;

    private final long behandlingID = 123;
    private final Long gsakSaksnummer = 1111L;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;
    private Fagsak fagsak;
    private MelosysEessiMelding melosysEessiMelding;
    private Prosessinstans prosessinstans;

    @Before
    public void setup() throws IkkeFunnetException {
        arbeidFlereLandSedRuter = new ArbeidFlereLandSedRuter(prosessinstansService, fagsakService, behandlingService, behandlingsresultatService, oppgaveService);

        behandling = new Behandling();
        behandling.setId(behandlingID);
        fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(behandling));
        behandling.setFagsak(fagsak);

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
    public void finnsakOgBestemRuting_utenArkivsaknummer_forventNySakRuting() throws MelosysException {
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(prosessinstansService).opprettProsessinstansNySakArbeidFlereLand(
            eq(melosysEessiMelding), eq(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND), eq(melosysEessiMelding.getAktoerId())
        );
    }

    @Test
    public void finnsakOgBestemRuting_fagsakEksistererIkke_kasterException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> arbeidFlereLandSedRuter.rutSedTilBehandling(new Prosessinstans(), 0L))
            .withMessageContaining("Finner ingen sak tilknyttet");
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakIkkeFattet_forventNyBehandling() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingArbeidFlereLand(
            eq(melosysEessiMelding), eq(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND), eq(gsakSaksnummer)
        );
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakFattet_kasterException() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandlingsresultat.setVedtakMetadata(new VedtakMetadata());
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer))
            .withMessageContaining("Det er allerede fattet vedtak på behandling");
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaNorgeUtpekt_forventNyBehandling() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingArbeidFlereLand(
            eq(melosysEessiMelding), eq(Behandlingstema.BESLUTNING_LOVVALG_NORGE), eq(gsakSaksnummer)
        );
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv_forventOppgaveOpprettetOgProsessinstansJfr() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(behandling), eq(melosysEessiMelding));
    }

    @Test
    public void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv_forventIngenBehandlingStatusVurderDokument() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(behandlingService).oppdaterStatus(eq(behandlingID), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(behandling), eq(melosysEessiMelding));
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode_forventIngenBehandling() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        melosysEessiMelding.setPeriode(new Periode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom()));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(behandling), eq(melosysEessiMelding));
    }

    @Test
    public void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode_forventNyBehandling() throws MelosysException {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        melosysEessiMelding.setPeriode(new Periode(lovvalgsperiode.getFom(), lovvalgsperiode.getTom().plusDays(1)));
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingArbeidFlereLand(
            eq(melosysEessiMelding), eq(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND), eq(gsakSaksnummer)
        );
    }
}