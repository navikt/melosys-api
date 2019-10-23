package no.nav.melosys.saksflyt.steg.jfr.sed;

import java.util.Collections;
import java.util.Optional;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettNyBehandlingTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private GsakFasade gsakFasade;

    private OpprettNyBehandling opprettNyBehandling;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        opprettNyBehandling = new OpprettNyBehandling(fagsakService, behandlingService, gsakFasade);
    }

    @Test
    public void utfør_gsakSaksnummerIkkeSatt_forventException() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Gsaksaksnummer kan ikke være null");

        opprettNyBehandling.utfør(prosessinstans);
    }

    @Test
    public void utfør_behandlingstypeIkkeSatt_forventException() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, 123L);

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Behandlingstype kan ikke være null");

        opprettNyBehandling.utfør(prosessinstans);
    }


    @Test
    public void utfør_harTidligereBehandlingOgOppgave_nyBehandlingOpprettet() throws MelosysException {
        final long gsakSaksnummer = 123L;
        final Behandlingstyper behandlingstype = Behandlingstyper.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        final String journalpostID = "jp123";
        final String dokumentID = "dok123";

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, gsakSaksnummer);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, behandlingstype);
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

        when(fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer))
            .thenReturn(Optional.of(fagsak));
        when(behandlingService.nyBehandling(any(), any(), any(), any(),any())).thenReturn(new Behandling());
        when(gsakFasade.finnOppgaverMedSaksnummer(eq(fagsak.getSaksnummer())))
            .thenReturn(Collections.singletonList(oppgave));

        opprettNyBehandling.utfør(prosessinstans);

        verify(gsakFasade).ferdigstillOppgave(eq(oppgave.getOppgaveId()));
        verify(behandlingService).avsluttBehandling(eq(behandling.getId()));
        verify(behandlingService).nyBehandling(
            eq(fagsak), eq(Behandlingsstatus.UNDER_BEHANDLING), eq(behandlingstype), eq(journalpostID), eq(dokumentID)
        );
        assertThat(prosessinstans.getBehandling()).isNotNull();
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_FERDIGSTILL_JOURNALPOST);


    }


}