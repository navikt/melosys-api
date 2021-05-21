package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;
import java.util.Arrays;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BrevDataByggerHenleggelseTest {
    @Mock
    JoarkService joarkService;

    private BrevDataByggerHenleggelse brevDataByggerHenleggelse;

    @BeforeEach
    public void setUp() {
        brevDataByggerHenleggelse = new BrevDataByggerHenleggelse(joarkService, new BrevbestillingDto());
    }

    @Test
    public void lag_henleggelseBrev_setterForendelseMottatt() {
        String journalpostId = "998877";

        Instant forsendelseMottatt = Instant.parse("2006-09-22T01:02:00Z");
        Journalpost journalpost = new Journalpost(journalpostId);
        journalpost.setForsendelseMottatt(forsendelseMottatt);
        doReturn(journalpost).when(joarkService).hentJournalpost(journalpostId);

        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setRegistrertDato(Instant.parse("2007-12-03T10:15:30.00Z"));
        førsteBehandling.setInitierendeJournalpostId(journalpostId);

        Behandling sisteBehandling = new Behandling();
        sisteBehandling.setRegistrertDato(Instant.parse("2017-12-10T10:15:45.00Z"));

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger(Arrays.asList(sisteBehandling, førsteBehandling));
        sisteBehandling.setFagsak(fagsak);
        BrevDataGrunnlag dataGrunnlag = mock(BrevDataGrunnlag.class);
        when(dataGrunnlag.getBehandling()).thenReturn(sisteBehandling);

        String saksbehandler = "saksbehandler";
        BrevDataMottattDato brevData = (BrevDataMottattDato) brevDataByggerHenleggelse.lag(dataGrunnlag, saksbehandler);

        assertThat(brevData.saksbehandler).isEqualTo(saksbehandler);
        assertThat(brevData.initierendeJournalpostForsendelseMottattTidspunkt).isEqualTo(forsendelseMottatt);
    }
}
