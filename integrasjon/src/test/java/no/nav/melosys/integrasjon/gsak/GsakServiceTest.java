package no.nav.melosys.integrasjon.gsak;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class GsakServiceTest {

    @Mock
    private OppgaveConsumer oppgaveConsumer;
    @Mock
    private SakConsumer sakConsumer;
    @Captor
    private ArgumentCaptor<OpprettOppgaveDto> oppgaveDtoCaptor;

    private GsakService instans;

    @Before
    public void setup() {
        instans = new GsakService(sakConsumer, oppgaveConsumer);
    }

    @Test
    public final void tildelIkkeEksisterendeOppgaveGirIkkeFunnetException() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.tildelOppgave("1", "2"));
        assertThat(unntak)
                .isInstanceOf(IkkeFunnetException.class)
                .hasMessageContaining("Feil")
                .hasMessageContaining("oppgave 1 for saksbehandler 2");
    }

    @Test
    public void opprettOppgave_gyldigOppgave_validerDto() throws Exception {
        Oppgave oppgave = lagOppgave();

        instans.opprettOppgave(oppgave);
        verify(oppgaveConsumer, times(1)).opprettOppgave(oppgaveDtoCaptor.capture());

        OpprettOppgaveDto oppgaveDto = oppgaveDtoCaptor.getValue();

        assertThat(oppgaveDto).isNotNull();
        assertThat(oppgaveDto.getJournalpostId()).isEqualTo(oppgave.getJournalpostId());
        assertThat(oppgaveDto.getAktørId()).isEqualTo(oppgave.getAktørId());
        assertThat(oppgaveDto.getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(oppgaveDto.getBehandlingstype()).isEqualTo("ae0034");
        assertThat(oppgaveDto.getOppgavetype()).isEqualTo(oppgave.getOppgavetype().getKode());
        assertThat(oppgaveDto.getPrioritet()).isEqualTo(PrioritetType.NORM.toString());
        assertThat(oppgaveDto.getTema()).isEqualTo(oppgave.getTema().getKode());
        assertThat(oppgaveDto.getTildeltEnhetsnr()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));
        assertThat(oppgaveDto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
    }

    private Oppgave lagOppgave() {
        Oppgave oppgave = new Oppgave();
        oppgave.setAktørId("aktoer123");
        oppgave.setBehandlingstype(Behandlingstyper.SOEKNAD);
        oppgave.setOppgavetype(Oppgavetyper.BEH_SAK);
        oppgave.setJournalpostId("journalpost123");
        oppgave.setSaksnummer("sak123");
        oppgave.setTema(Tema.MED);
        oppgave.setTilordnetRessurs("ressurs123");

        return oppgave;
    }

}
