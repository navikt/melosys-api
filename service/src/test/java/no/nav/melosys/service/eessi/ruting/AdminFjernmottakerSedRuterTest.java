package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private AdminFjernmottakerSedRuter adminFjernmottakerSedRuter;

    private final long behandlingID = 111;
    private final long arkivsakID = 123321;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
    private final String rinaSaksnummer = "1233333";
    private final String sedID = "2414";

    @BeforeEach
    void setup() {
        adminFjernmottakerSedRuter = new AdminFjernmottakerSedRuter(fagsakService, prosessinstansService, oppgaveService,
            behandlingsresultatService, medlPeriodeService);

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
    void rutSedTilBehandling_fjernMottakerSomIkkeErNorskPåÅpenA003_blirIkkeAvsluttetEllerSattTilAnnullert() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING);
        Institusjon danskInstitusjon = new Institusjon("DK:DENNAV006", "NavIDanmark", Landkoder.DE.name());
        melosysEessiMelding.setInstitusjon(danskInstitusjon);
        Behandling sistAktiveBehandling = fagsak.hentSistAktiveBehandling();

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(fagsak.hentSistAktiveBehandling(), melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_fjernMottakerSomErNorskPåÅpenA003_blirAvsluttetOgSattTilAnnullert() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING);
        Institusjon danskInstitusjon = new Institusjon("NO:NAVAT07", "NONAVAT07", Landkoder.NO.name());
        melosysEessiMelding.setInstitusjon(danskInstitusjon);

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Behandling sistAktiveBehandling = fagsak.hentSistAktiveBehandling();

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(sistAktiveBehandling);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setMedlPeriodeID(20L);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        adminFjernmottakerSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT);
        verify(medlPeriodeService).avvisPeriodeOpphørt(behandlingsresultat.hentValidertAnmodningsperiode().getMedlPeriodeID());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
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
