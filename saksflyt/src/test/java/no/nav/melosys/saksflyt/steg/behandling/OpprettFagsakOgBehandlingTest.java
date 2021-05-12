package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettFagsakOgBehandlingTest {
    @Mock
    private FagsakService fagsakService;
    @Mock
    private PersondataFasade persondataFasade;

    private OpprettFagsakOgBehandling opprettFagsakOgBehandling;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @BeforeEach
    public void setUp() {
        opprettFagsakOgBehandling = new OpprettFagsakOgBehandling(fagsakService, persondataFasade);
    }

    @Test
    void utfør_typeJfrNySak_fagsakBliropprettet() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        String aktørId = "1000104568393";
        String journalpostId = "44553";
        String dokumentId = "222221";
        String arbeidsgiver = "104568393";
        String representant = "rep";
        String representantKontaktperson = "kontaktperson";
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørId);
        prosessinstans.setData(ProsessDataKey.ARBEIDSGIVER, arbeidsgiver);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, dokumentId);
        prosessinstans.setData(ProsessDataKey.REPRESENTANT, representant);
        prosessinstans.setData(ProsessDataKey.REPRESENTANT_KONTAKTPERSON, representantKontaktperson);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        opprettFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue().getAktørID()).isEqualTo(aktørId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getInitierendeJournalpostId()).isEqualTo(journalpostId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getInitierendeDokumentId()).isEqualTo(dokumentId);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getFullmektig().getRepresentantID())
            .isEqualTo(representant);
        assertThat(opprettSakRequestArgumentCaptor.getValue().getKontaktopplysninger().get(0).getKontaktNavn())
            .isEqualTo(representantKontaktperson);
    }
}
