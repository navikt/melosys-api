package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsaarsak;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
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

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
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
        prosessinstans.setType(ProsessType.JFR_NY_SAK_BRUKER);
        String aktørId = "1000104568393";
        String journalpostId = "44553";
        String dokumentId = "222221";
        String arbeidsgiver = "104568393";
        String representant = "rep";
        String representantKontaktperson = "kontaktperson";
        prosessinstans.setData(AKTØR_ID, aktørId);
        prosessinstans.setData(ARBEIDSGIVER, arbeidsgiver);
        prosessinstans.setData(JOURNALPOST_ID, journalpostId);
        prosessinstans.setData(DOKUMENT_ID, dokumentId);
        prosessinstans.setData(REPRESENTANT, representant);
        prosessinstans.setData(REPRESENTANT_KONTAKTPERSON, representantKontaktperson);
        prosessinstans.setData(BEHANDLINGSTEMA, Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.setData(SAKSTYPE, Sakstyper.EU_EOS);
        prosessinstans.setData(SAKSTEMA, Sakstemaer.MEDLEMSKAP_LOVVALG);
        prosessinstans.setData(BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.FRITEKST);
        prosessinstans.setData(BEHANDLINGSÅRSAK_FRITEKST, "Fritekst");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        opprettFagsakOgBehandling.utfør(prosessinstans);

        verify(fagsakService).nyFagsakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        OpprettSakRequest opprettSakRequest = opprettSakRequestArgumentCaptor.getValue();
        assertThat(opprettSakRequest.getAktørID()).isEqualTo(aktørId);
        assertThat(opprettSakRequest.getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(opprettSakRequest.getInitierendeJournalpostId()).isEqualTo(journalpostId);
        assertThat(opprettSakRequest.getInitierendeDokumentId()).isEqualTo(dokumentId);
        assertThat(opprettSakRequest.getFullmektig().getRepresentantID())
            .isEqualTo(representant);
        assertThat(opprettSakRequest.getKontaktopplysninger().get(0).getKontaktNavn())
            .isEqualTo(representantKontaktperson);
        assertThat(opprettSakRequest.getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(opprettSakRequest.getSakstema()).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(opprettSakRequest.getBehandlingsårsaktype()).isEqualTo(Behandlingsaarsaktyper.FRITEKST);
        assertThat(opprettSakRequest.getBehandlingsårsakFritekst()).isEqualTo("Fritekst");
    }
}
