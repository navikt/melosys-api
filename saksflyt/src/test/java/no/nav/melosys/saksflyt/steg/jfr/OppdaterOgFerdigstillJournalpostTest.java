package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.service.sak.FagsakService;
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
class OppdaterOgFerdigstillJournalpostTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;

    private OppdaterOgFerdigstillJournalpost oppdaterOgFerdigstillJournalpost;

    @Captor
    private ArgumentCaptor<JournalpostOppdatering> oppdateringArgumentCaptor;

    @BeforeEach
    public void setUp() {
        oppdaterOgFerdigstillJournalpost = new OppdaterOgFerdigstillJournalpost(joarkFasade, fagsakService);
    }

    @Test
    void utfoerSteg() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, "navn");
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.getBehandling().setFagsak(new Fagsak());
        prosessinstans.getBehandling().getFagsak().setSaksnummer("MEL-123");
        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterJournalpost(any(), oppdateringArgumentCaptor.capture(), eq(true));
    }

    @Test
    void utfoerSteg_behandlingIkkeSatt_henterSaksnummerFraData() throws MelosysException {
        Fagsak fagsak = new Fagsak();
        long gsakSaksnummer = 10L;
        String saksnummer = "saksnummer";
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        fagsak.setSaksnummer(saksnummer);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, "navn");
        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterJournalpost(any(), oppdateringArgumentCaptor.capture(), eq(true));
        assertThat(oppdateringArgumentCaptor.getValue().getSaksnummer()).isEqualTo(saksnummer);
    }
}
