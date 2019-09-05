package no.nav.melosys.integrasjon.gsak;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
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
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class GsakServiceTest {

    @Mock
    private OppgaveConsumer oppgaveConsumer;
    @Mock
    private SakConsumer sakConsumer;
    @Captor
    private ArgumentCaptor<OpprettOppgaveDto> oppgaveDtoCaptor;
    @Captor
    private ArgumentCaptor<OppgaveSearchRequest> oppgaveSearchRequestCaptor;

    private GsakService gsakService;

    @Before
    public void setup() {
        gsakService = new GsakService(sakConsumer, oppgaveConsumer);
    }

    @Test
    public final void tildelIkkeEksisterendeOppgaveGirIkkeFunnetException() {
        Throwable unntak = catchThrowable(() -> gsakService.tildelOppgave("1", "2"));
        assertThat(unntak)
                .isInstanceOf(IkkeFunnetException.class)
                .hasMessageContaining("Feil")
                .hasMessageContaining("oppgave 1 for saksbehandler 2");
    }

    @Test
    public void opprettOppgave_vurderDokument_setterData() throws Exception {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgavetype(Oppgavetyper.VUR);
        oppgaveBuilder.setTema(Tema.MED);
        oppgaveBuilder.setBehandlingstema(Behandlingstema.EU_EOS);
        gsakService.opprettOppgave(oppgaveBuilder.build());

        ArgumentCaptor<OpprettOppgaveDto> captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveConsumer).opprettOppgave(captor.capture());
        OpprettOppgaveDto opprettOppgaveDto = captor.getValue();

        assertThat(opprettOppgaveDto.getOppgavetype()).isEqualTo(Oppgavetyper.VUR.getKode());
        assertThat(opprettOppgaveDto.getBehandlingstema()).isEqualTo(Behandlingstema.EU_EOS.getKode());
        assertThat(opprettOppgaveDto.getFristFerdigstillelse()).isNotNull();
    }

    @Test
    public void opprettOppgave_gyldigOppgave_validerDto() throws Exception {
        Oppgave oppgave = lagOppgave();

        gsakService.opprettOppgave(oppgave);
        verify(oppgaveConsumer).opprettOppgave(oppgaveDtoCaptor.capture());

        OpprettOppgaveDto oppgaveDto = oppgaveDtoCaptor.getValue();

        assertThat(oppgaveDto).isNotNull();
        assertThat(oppgaveDto.getJournalpostId()).isEqualTo(oppgave.getJournalpostId());
        assertThat(oppgaveDto.getAktørId()).isEqualTo(oppgave.getAktørId());
        assertThat(oppgaveDto.getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(oppgaveDto.getBehandlingstype()).isEqualTo("ae0034");
        assertThat(oppgaveDto.getBeskrivelse()).isEqualTo("bla bla");
        assertThat(oppgaveDto.getOppgavetype()).isEqualTo(oppgave.getOppgavetype().getKode());
        assertThat(oppgaveDto.getPrioritet()).isEqualTo(PrioritetType.NORM.toString());
        assertThat(oppgaveDto.getTema()).isEqualTo(oppgave.getTema().getKode());
        assertThat(oppgaveDto.getTildeltEnhetsnr()).isEqualTo(Integer.toString(Konstanter.MELOSYS_ENHET_ID));
        assertThat(oppgaveDto.getTilordnetRessurs()).isEqualTo(oppgave.getTilordnetRessurs());
    }

    @Test
    public void finnOppgaveListeMedAnsvarlig_gyldigOppgave_verifiserToKallMotOppgave() throws Exception {
        OppgaveDto oppgaveDto = new OppgaveDto();
        when(oppgaveConsumer.hentOppgaveListe(any(OppgaveSearchRequest.class))).thenReturn(Collections.singletonList(oppgaveDto));

        gsakService.finnOppgaveListeMedAnsvarlig("123");
        verify(oppgaveConsumer, times(2)).hentOppgaveListe(oppgaveSearchRequestCaptor.capture());

        List<OppgaveSearchRequest> requests = oppgaveSearchRequestCaptor.getAllValues();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0).getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.MELOSYS.getKode());
        assertThat(requests.get(0).getOppgavetype()).isNullOrEmpty();
        assertThat(requests.get(1).getBehandlesAvApplikasjon()).isNullOrEmpty();
        assertThat(requests.get(1).getOppgavetype()[0]).isEqualTo(Oppgavetyper.JFR.getKode());
    }

    private Oppgave lagOppgave() {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setAktørId("aktoer123");
        oppgaveBuilder.setBehandlingstype(Behandlingstyper.SOEKNAD);
        oppgaveBuilder.setBeskrivelse("bla bla");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setJournalpostId("journalpost123");
        oppgaveBuilder.setSaksnummer("sak123");
        oppgaveBuilder.setTema(Tema.MED);
        oppgaveBuilder.setTilordnetRessurs("ressurs123");

        return oppgaveBuilder.build();
    }
}
