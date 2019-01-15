package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;

import no.nav.melosys.audit.AuditorProvider;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.ProsessDataKey.DOKUMENT_ID;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private FagsakService fagsakService;

    @Mock
    private BehandlingService behandlingService;

    private OpprettFagsakOgBehandling agent;

    private ApplicationEventPublisher applicationEventPublisher;


    @Before
    public void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        AuditorProvider auditorAware = mock(AuditorProvider.class);
        agent = new OpprettFagsakOgBehandling(fagsakService, behandlingService, auditorAware);
    }

    @Test
    public void utførSteg_typeJfrNySak_tilStegJfrOpprettSøknad() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        String aktørId = "FJERNET93";
        String journalpostId = "44553";
        String dokumentId = "222221";
        String arbeidsgiver = "104568393";
        p.setData(ProsessDataKey.AKTØR_ID, aktørId);
        p.setData(ProsessDataKey.ARBEIDSGIVER, arbeidsgiver);
        p.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        p.setData(ProsessDataKey.DOKUMENT_ID, dokumentId);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(anyString(), anyString(), any(), eq(Behandlingstype.SØKNAD), any(), any())).thenReturn(fagsak);

        agent.utførSteg(p);

        verify(fagsakService).nyFagsakOgBehandling(aktørId, arbeidsgiver, null, Behandlingstype.SØKNAD, journalpostId, dokumentId);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SØKNAD);
    }

    @Test
    public void utførSteg_typeJfrNyBehandling_tilStegStatusBehOppr() {
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";

        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");
        p.setData(JOURNALPOST_ID, initierendeJournalpostId);
        p.setData(DOKUMENT_ID, initierendeDokumentId);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        when(fagsakService.hentFagsak("MELTEST-333")).thenReturn(fagsak);
        when(behandlingService.nyBehandling(eq(fagsak), any(), any(), anyString(), anyString())).thenReturn(new Behandling());

        agent.utførSteg(p);

        verify(behandlingService).nyBehandling(fagsak, Behandlingsstatus.VURDER_DOKUMENT, Behandlingstype.SØKNAD, initierendeJournalpostId, initierendeDokumentId);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
    }

    @Test
    public void utførSteg_ukjentType_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }
}