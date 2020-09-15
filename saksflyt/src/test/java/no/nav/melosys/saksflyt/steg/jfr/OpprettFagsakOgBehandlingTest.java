package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
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
    private TpsFasade tpsFasade;

    private OpprettFagsakOgBehandling agent;

    @Captor
    private ArgumentCaptor<OpprettSakRequest> opprettSakRequestArgumentCaptor;

    @BeforeEach
    public void setUp() {
        agent = new OpprettFagsakOgBehandling(fagsakService, tpsFasade);
    }

    @Test
    void utfør_typeJfrNySak_fagsakBliropprettet() throws FunksjonellException, TekniskException {
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
    }
}