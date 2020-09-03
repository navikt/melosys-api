package no.nav.melosys.saksflyt.steg.jfr;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DOKUMENT_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.JOURNALPOST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private OpprettFagsakOgBehandling agent;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @Before
    public void setUp() {
        agent = new OpprettFagsakOgBehandling(fagsakService, behandlingService);
    }

    @Test
    public void utfør_typeJfrNySak_tilStegJfrOpprettSøknad() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        String aktørId = "1000104568393";
        String journalpostId = "44553";
        String dokumentId = "222221";
        String arbeidsgiver = "104568393";
        String representant = "rep";
        String representantKontaktperson = "kontaktperson";
        p.setData(ProsessDataKey.AKTØR_ID, aktørId);
        p.setData(ProsessDataKey.ARBEIDSGIVER, arbeidsgiver);
        p.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        p.setData(ProsessDataKey.DOKUMENT_ID, dokumentId);
        p.setData(ProsessDataKey.REPRESENTANT, representant);
        p.setData(ProsessDataKey.REPRESENTANT_KONTAKTPERSON, representantKontaktperson);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        agent.utfør(p);

        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getAktørID()).isEqualTo(aktørId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getInitierendeJournalpostId()).isEqualTo(journalpostId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getInitierendeDokumentId()).isEqualTo(dokumentId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getFullmektig().getRepresentantID())
            .isEqualTo(representant);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getKontaktopplysninger().get(0).getKontaktNavn())
            .isEqualTo(representantKontaktperson);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SØKNAD);
    }

    @Test
    public void utfør_typeNySakFraDok_tilStegJfrOpprettSøknad() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPRETT_NY_SAK);
        String aktørId = "1000104568393";
        p.setData(ProsessDataKey.AKTØR_ID, aktørId);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        agent.utfør(p);

        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getAktørID()).isEqualTo(aktørId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getInitierendeJournalpostId()).isNull();

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SØKNAD);
    }

    @Test
    public void utfør_typeJfrNyBehandling_tilStegStatusBehOppr() throws FunksjonellException, TekniskException {
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";

        Behandling sistOppdaterteBehandling = new Behandling();
        sistOppdaterteBehandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        sistOppdaterteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        sistOppdaterteBehandling.setEndretDato(Instant.now());

        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");
        p.setData(JOURNALPOST_ID, initierendeJournalpostId);
        p.setData(DOKUMENT_ID, initierendeDokumentId);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(List.of(sistOppdaterteBehandling));
        when(fagsakService.hentFagsak("MELTEST-333")).thenReturn(fagsak);
        when(behandlingService.nyBehandling(eq(fagsak), any(), any(), any(), anyString(), anyString())).thenReturn(new Behandling());

        agent.utfør(p);

        verify(behandlingService).nyBehandling(fagsak, Behandlingsstatus.VURDER_DOKUMENT, Behandlingstyper.SOEKNAD, sistOppdaterteBehandling.getTema(), initierendeJournalpostId, initierendeDokumentId);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
    }

    @Test
    public void utfør_ukjentType_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> agent.utfør(p))
            .withMessageContaining("er ikke støttet");
    }
}