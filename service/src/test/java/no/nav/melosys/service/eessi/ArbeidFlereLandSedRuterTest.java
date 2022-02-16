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
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.ArbeidFlereLandSedRuter;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArbeidFlereLandSedRuterTest {

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

    @BeforeEach
    public void setup() {
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

        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer)).thenReturn(Optional.of(fagsak));
    }

    @Test
    void finnsakOgBestemRuting_utenArkivsaknummer_forventNySakRuting() {
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(prosessinstansService).opprettProsessinstansNySakArbeidFlereLand(
            melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, melosysEessiMelding.getAktoerId()
        );
    }

    @Test
    void finnsakOgBestemRuting_fagsakEksistererIkke_kasterException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> arbeidFlereLandSedRuter.rutSedTilBehandling(new Prosessinstans(), 0L))
            .withMessageContaining("Finner ingen sak tilknyttet");
    }

    @Test
    void finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakIkkeFattet_forventNyBehandling() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingArbeidFlereLand(
            melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, gsakSaksnummer
        );
    }

    @Test
    void finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakFattet_kasterException() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandlingsresultat.setVedtakMetadata(new VedtakMetadata());
        melosysEessiMelding.setLovvalgsland(Landkoder.SE.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer))
            .withMessageContaining("Det er allerede fattet vedtak på behandling");
    }

    @Test
    void finnSakOgBestemRuting_annetLandUtpektNyttTemaNorgeUtpekt_forventNyBehandling() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(prosessinstansService).opprettProsessinstansNyBehandlingArbeidFlereLand(
            melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_NORGE, gsakSaksnummer
        );
    }

    @Test
    void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv_forventOppgaveOpprettetOgProsessinstansJfr() {
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding);
    }

    @Test
    void finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv_forventIngenBehandlingStatusVurderDokumentOppdaterOppgave() {
        final var oppgaveID = "4231432";
        final var oppgaveOppdateringCaptor = ArgumentCaptor.forClass(OppgaveOppdatering.class);

        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        melosysEessiMelding.setLovvalgsland(Landkoder.NO.getKode());
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(fagsak.getSaksnummer()))
            .thenReturn(Optional.of(new Oppgave.Builder().setOppgaveId(oppgaveID).build()));

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer);

        verify(behandlingService).endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(oppgaveService).oppdaterOppgave(eq(oppgaveID), oppgaveOppdateringCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding);

        assertThat(oppgaveOppdateringCaptor.getValue())
            .extracting(OppgaveOppdatering::getBeskrivelse)
            .isEqualTo("Mottatt SED A003");
    }

    @Test
    void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode_forventIngenBehandling() {
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

        verify(prosessinstansService).opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding);
    }

    @Test
    void finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode_forventNyBehandling() {
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
            melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, gsakSaksnummer
        );
    }
}
