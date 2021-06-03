package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettNyBehandlingFraSedTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveFasade;

    private OpprettNyBehandlingFraSed opprettNyBehandlingFraSed;

    @BeforeEach
    public void setup() {
        opprettNyBehandlingFraSed = new OpprettNyBehandlingFraSed(fagsakService, behandlingService, oppgaveFasade);
    }

    @Test
    void utfør_gsakSaksnummerIkkeSatt_forventException() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> opprettNyBehandlingFraSed.utfør(prosessinstans))
            .withMessageContaining("ArkivsakID kan ikke være null");
    }

    @Test
    void utfør_behandlingstypeIkkeSatt_forventException() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123L);

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> opprettNyBehandlingFraSed.utfør(prosessinstans))
            .withMessageContaining("Behandlingstema kan ikke være null");
    }


    @Test
    void utfør_harTidligereBehandlingOgOppgave_nyBehandlingOpprettet() {
        final long gsakSaksnummer = 123L;
        final Behandlingstema behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        final String journalpostID = "jp123";
        final String dokumentID = "dok123";

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, behandlingstema);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, dokumentID);

        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-199001");
        fagsak.setBehandlinger(Lists.newArrayList(behandling));

        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("123oppg")
            .build();

        when(fagsakService.hentFagsakFraArkivsakID(gsakSaksnummer)).thenReturn(fagsak);
        when(behandlingService.nyBehandling(any(), any(), any(), any(), any(), any())).thenReturn(new Behandling());
        when(oppgaveFasade.finnÅpenOppgaveMedFagsaksnummer(eq(fagsak.getSaksnummer())))
            .thenReturn(Optional.of(oppgave));

        opprettNyBehandlingFraSed.utfør(prosessinstans);

        verify(oppgaveFasade).ferdigstillOppgave(eq(oppgave.getOppgaveId()));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
        verify(behandlingService).nyBehandling(
            eq(fagsak), eq(Behandlingsstatus.UNDER_BEHANDLING), eq(Behandlingstyper.SED), eq(behandlingstema), eq(journalpostID), eq(dokumentID)
        );
        assertThat(prosessinstans.getBehandling()).isNotNull();
    }
}
