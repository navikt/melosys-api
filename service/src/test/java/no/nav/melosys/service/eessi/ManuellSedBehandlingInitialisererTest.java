package no.nav.melosys.service.eessi;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private GsakFasade gsakFasade;

    private ManuellSedBehandlingInitialiserer manuellSedBehandlingInitialiserer;

    private static final String SAKSNUMMER = "MEL-!!!";
    private static final Long GSAK_SAKSNUMMER = 123L;

    @Before
    public void setup() {
        manuellSedBehandlingInitialiserer = new ManuellSedBehandlingInitialiserer(fagsakService, behandlingService, gsakFasade);
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
        verify(gsakFasade, never()).finnOppgaverMedSaksnummer(any());
    }

    @Test
    public void bestemManuellBehandling_X009PurringSaksnummerOgFagsakEksisterer_oppdatererPrioritet() throws FunksjonellException, TekniskException {

        final String oppgaveId = "333";
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(SedType.X009);
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(oppgaveId).build();

        when(fagsakService.hentFagsakFraGsakSaksnummer(GSAK_SAKSNUMMER)).thenReturn(hentFagsak());
        when(gsakFasade.finnOppgaverMedSaksnummer(eq(SAKSNUMMER))).thenReturn(Collections.singletonList(oppgave));
        manuellSedBehandlingInitialiserer.bestemManuellBehandling(prosessinstans, melosysEessiMelding);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.MOTTAK_SED_JOURNALFØRING);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);
        assertThat(prosessinstans.getBehandling()).isNotNull();
        verify(behandlingService).oppdaterStatus(anyLong(), eq(Behandlingsstatus.VURDER_DOKUMENT));
        verify(gsakFasade).finnOppgaverMedSaksnummer(eq(SAKSNUMMER));
        verify(gsakFasade).oppdaterOppgavePrioritet(eq(oppgaveId), eq(PrioritetType.HOY));

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

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getBehandlinger().add(behandling);
        return fagsak;
    }
}