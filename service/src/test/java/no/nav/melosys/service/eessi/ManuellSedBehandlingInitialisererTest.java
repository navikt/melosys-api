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
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManuellSedBehandlingInitialisererTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    private ManuellSedBehandlingInitialiserer manuellSedBehandlingInitialiserer;

    private static final String SAKSNUMMER = "MEL-!!!";
    private static final Long GSAK_SAKSNUMMER = 123L;

    @Before
    public void setup() {
        manuellSedBehandlingInitialiserer = new ManuellSedBehandlingInitialiserer(fagsakService, behandlingService, oppgaveService);
    }

    @Test
    public void bestemManuellBehandling_saksnummerFinnesIkkeErNySedINyBehandling_nesteStegOppretJfrOppg() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A005);
        manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_OPPRETT_JFR_OPPG);
    }

    @Test
    public void bestemManuellBehandling_saksnummerOgFagsakEksisterer_nesteStegFerdigstillJournalpost() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.H002);

        when(fagsakService.hentFagsakFraGsakSaksnummer(GSAK_SAKSNUMMER)).thenReturn(hentFagsak());
        manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SED_JOURNALFØRING);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
    }

    @Test
    public void bestemManuellBehandling_X009PurringSaksnummerOgFagsakEksisterer_oppdatererPrioritet() throws FunksjonellException, TekniskException {

        final String oppgaveId = "333";
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(oppgaveId).build();

        when(fagsakService.hentFagsakFraGsakSaksnummer(GSAK_SAKSNUMMER)).thenReturn(hentFagsak());
        when(oppgaveService.finnOppgaveMedFagsaksnummer(eq(SAKSNUMMER))).thenReturn(Optional.of(oppgave));
        manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SED_JOURNALFØRING);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(oppgaveService).finnOppgaveMedFagsaksnummer(eq(SAKSNUMMER));
        verify(oppgaveService).oppdaterOppgave(eq(oppgaveId), any(OppgaveOppdatering.class));
    }

    @Test
    public void bestemManuellBehandling_behandlingAvsluttetOgSkalBehandleSED_opprettOppgave() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.A004);

        Fagsak fagsak = hentFagsak();
        fagsak.getAktivBehandling().setStatus(Behandlingsstatus.AVSLUTTET);
        when(fagsakService.hentFagsakFraGsakSaksnummer(GSAK_SAKSNUMMER)).thenReturn(fagsak);

        manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);

        ArgumentCaptor<Oppgave> oppgaveCaptor = ArgumentCaptor.forClass(Oppgave.class);
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", SAKSNUMMER)
            .hasFieldOrPropertyWithValue("beskrivelse", "Mottatt SED A004");
    }

    private MelosysEessiMelding hentMelosysEessiMelding(SedType sedType) {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType(sedType.name());
        return melosysEessiMelding;
    }

    private Fagsak hentFagsak() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setTema(Behandlingstema.ØVRIGE_SED);
        behandling.setType(Behandlingstyper.SOEKNAD);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getBehandlinger().add(behandling);
        return fagsak;
    }
}