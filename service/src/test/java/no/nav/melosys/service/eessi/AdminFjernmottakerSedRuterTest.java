package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.eessi.ruting.AdminFjernmottakerSedRuter;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFjernmottakerSedRuterTest {
    @Mock
    private FagsakService fagsakService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private BehandlingService behandlingService;

    private AdminFjernmottakerSedRuter adminFjernmottakerSedRuter;

    private final long behandlingID = 111;
    private final long arkivsakID = 123321;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();

    @BeforeEach
    void setup() {
        adminFjernmottakerSedRuter = new AdminFjernmottakerSedRuter(fagsakService, prosessinstansService, oppgaveService,
            behandlingsresultatService, medlPeriodeService, behandlingService);

        melosysEessiMelding.setAktoerId("12312412");
        melosysEessiMelding.setRinaSaksnummer("143141");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_arkivsaksIdErNull_opprettJournalFøringsOppgave() {
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());

    }

    @Test
    void rutSedTilBehandling_finnesIngenTilhørendeFagsak_opprettesJfrOppgave() {
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void rutSedTilBehandling_erIkkeX006MottakerPåÅpenA003_blirIkkeAvsluttetEllerSattTilAnnullert() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING);
        melosysEessiMelding.setX006NavErFjernet(false);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandling();

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_erX006MottakerErIkkeTilstedePåSed_opprettJournalFøringsProsess() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING);

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandling();

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));

        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verifyNoInteractions(behandlingsresultatService);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_erX006MottakerPåÅpenA003_blirAvsluttetOgSattTilAnnullert() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.UNDER_BEHANDLING);
        melosysEessiMelding.setX006NavErFjernet(true);

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandling();

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(sistAktiveBehandling);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setMedlPeriodeID(20L);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT);
        verify(medlPeriodeService).avvisPeriodeOpphørt(behandlingsresultat.hentAnmodningsperiode().getMedlPeriodeID());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_erX006MottakerPåAvsluttetBehandling_oppdaterStatusPåFagsakTilAnnulert() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET);
        melosysEessiMelding.setX006NavErFjernet(true);

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandling();

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(sistAktiveBehandling);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setMedlPeriodeID(20L);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).oppdaterStatus(fagsak, Saksstatuser.ANNULLERT);
        verify(medlPeriodeService).avvisPeriodeOpphørt(anmodningsperiode.getMedlPeriodeID());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektAktiv_behandlingsstausVURDER_DOKUMENT() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING);
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandling();


        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);


        verify(behandlingService).endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektIkkeAktiv_journalføringsOppgaveLages() {
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET)));


        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);


        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    private Behandling lagBehandling(Fagsak fagsak, Behandlingstema behandlingstema, Behandlingsstatus behandlingsstatus) {
        var behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setTema(behandlingstema);
        behandling.setEndretDato(Instant.now());
        behandling.setFagsak(fagsak);
        behandling.setStatus(behandlingsstatus);
        return behandling;
    }

    private Fagsak lagFagsak(Behandlingstema behandlingstema, Behandlingsstatus behandlingsstatus) {
        var fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(lagBehandling(fagsak, behandlingstema, behandlingsstatus)));
        return fagsak;
    }
}
