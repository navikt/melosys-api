package no.nav.melosys.integrasjon.gsak.oppgave;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakService;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.sakapi.SakApiConsumer;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppgaveMappingMellomDTOogDomainTest {

    private SakApiConsumer sakApiConsumerMock;

    private OppgaveConsumer oppgaveConsumerMock;

    private GsakFasade gsakFasade;

    @Before
    public void setUp() {
        sakApiConsumerMock = mock(SakApiConsumer.class);
        oppgaveConsumerMock = mock(OppgaveConsumer.class);
        gsakFasade = new GsakService(sakApiConsumerMock,oppgaveConsumerMock);
    }

    @Test
    public void testMappingMellomDTOogDomainForOppgave() throws MelosysException {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setId("1234");
        oppgaveDto.setSakreferanse("456");
        oppgaveDto.setOppgavetype("JFR");
        oppgaveDto.setTema("MED");
        when(oppgaveConsumerMock.hentOppgave("1234")).thenReturn(oppgaveDto);
        Oppgave oppgave = gsakFasade.hentOppgave("1234");
        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(oppgave.getGsakSaksnummer()).isEqualTo("456");
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetype.valueOf("JFR"));
        assertThat(oppgave.getTema()).isEqualTo(Tema.valueOf("MED"));
    }
}
