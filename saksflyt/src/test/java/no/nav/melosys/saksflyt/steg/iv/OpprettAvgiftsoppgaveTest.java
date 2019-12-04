package no.nav.melosys.saksflyt.steg.iv;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_AVSLUTT_BEHANDLING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettAvgiftsoppgaveTest {
    @Mock
    private GsakFasade gsakFasade;
    @Captor
    private ArgumentCaptor<Oppgave> oppgave;

    private OpprettAvgiftsoppgave opprettAvgiftsoppgave;

    private static final String DUMM_ID = "DUMM_ID";

    @Before
    public void setUp() {
        opprettAvgiftsoppgave = new OpprettAvgiftsoppgave(gsakFasade);
    }

    @Test
    public void utfør_oppretterRiktigOppgave() throws FunksjonellException, TekniskException {
        opprettAvgiftsoppgave.utfør(lagProsessinstans());

        verify(gsakFasade).opprettOppgave(oppgave.capture());
        assertThat(oppgave.getValue().getTema()).isEqualTo(Tema.TRY);
        assertThat(oppgave.getValue().getOppgavetype()).isEqualTo(Oppgavetyper.VUR);
        assertThat(oppgave.getValue().getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.INTET);
        assertThat(oppgave.getValue().getAktørId()).isEqualTo(DUMM_ID);
        assertThat(oppgave.getValue().getBeskrivelse()).isEqualTo(OpprettAvgiftsoppgave.AVGIFTSVURDERING_BESKRIVELSE);
    }

    @Test
    public void utfør_tilAvslutBehandling() throws FunksjonellException, TekniskException {
        Prosessinstans p = lagProsessinstans();
        opprettAvgiftsoppgave.utfør(p);
        assertThat(p.getSteg()).isEqualTo(IV_AVSLUTT_BEHANDLING);
    }

    private static Prosessinstans lagProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(lagBehandling(lagFagsak()));
        return prosessinstans;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.getAktører().add(lagBruker());
        return fagsak;
    }

    private static Aktoer lagBruker() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(DUMM_ID);
        return aktoer;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);
        return behandling;
    }
}