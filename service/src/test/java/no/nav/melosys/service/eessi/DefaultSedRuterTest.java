package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.eessi.ruting.DefaultSedRuter;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultSedRuterTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private DefaultSedRuter defaultSedRuter;

    private static final String SAKSNUMMER = "MEL-!!!";
    private static final Long GSAK_SAKSNUMMER = 123L;

    @BeforeEach
    public void setup() {
        defaultSedRuter = new DefaultSedRuter(prosessinstansService, fagsakService, behandlingService, oppgaveService);
    }

    @Test
    public void bestemManuellBehandling_saksnummerFinnesIkkeErNySedINyBehandling_nesteStegOppretJfrOppg() {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A005);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        defaultSedRuter.rutSedTilBehandling(prosessinstans, null);

        verify(oppgaveService).opprettJournalføringsoppgave(eq(melosysEessiMelding.getJournalpostId()), eq(melosysEessiMelding.getAktoerId()));
    }

    @Test
    public void bestemManuellBehandling_X009PurringSaksnummerOgFagsakEksisterer_oppdatererPrioritet() {

        final String oppgaveId = "333";
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(oppgaveId).build();
        Fagsak fagsak = hentFagsak();

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(eq(SAKSNUMMER))).thenReturn(Optional.of(oppgave));

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);

        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(fagsak.hentAktivBehandling()), eq(melosysEessiMelding));
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(oppgaveService).finnÅpenOppgaveMedFagsaksnummer(eq(SAKSNUMMER));
        verify(oppgaveService).oppdaterOppgave(eq(oppgaveId), any(OppgaveOppdatering.class));
    }

    @Test
    public void bestemManuellBehandling_A012SaksnummerOgFagsakEksistererStatusMidlertidigLovvalgsbeslutning_ikkeOppdaterStatusEllerOppgave() {

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A012);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = hentFagsak();
        fagsak.getSistOppdaterteBehandling().setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);

        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);
        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(fagsak.hentSistAktiveBehandling()), eq(melosysEessiMelding));
        verify(behandlingService, never()).oppdaterStatus(anyLong(), any());
        verify(oppgaveService, never()).finnÅpenOppgaveMedFagsaksnummer(any());
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

    @Test
    public void bestemManuellBehandling_behandlingAvsluttetOgSkalBehandleSED_opprettOppgave() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A004);
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);

        Fagsak fagsak = hentFagsak();
        fagsak.hentAktivBehandling().setStatus(Behandlingsstatus.AVSLUTTET);
        when(fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        defaultSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER);

        ArgumentCaptor<Oppgave> oppgaveCaptor = ArgumentCaptor.forClass(Oppgave.class);
        verify(behandlingService, never()).oppdaterStatus(anyLong(), any());
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(eq(fagsak.hentSistAktiveBehandling()), eq(melosysEessiMelding));
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", SAKSNUMMER)
            .hasFieldOrPropertyWithValue("beskrivelse", "Mottatt SED A004");
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(sedType.name());
        melosysEessiMelding.setAktoerId("12321321");
        return melosysEessiMelding;
    }

    private Fagsak hentFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setTema(Behandlingstema.ØVRIGE_SED_MED);
        behandling.setType(Behandlingstyper.SOEKNAD);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getBehandlinger().add(behandling);
        behandling.setFagsak(fagsak);
        return fagsak;
    }
}
