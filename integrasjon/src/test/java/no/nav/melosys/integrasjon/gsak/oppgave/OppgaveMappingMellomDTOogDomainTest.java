package no.nav.melosys.integrasjon.gsak.oppgave;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.GsakService;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppgaveMappingMellomDTOogDomainTest {

    private SakConsumer sakConsumerMock;

    private OppgaveConsumer oppgaveConsumerMock;

    private GsakFasade gsakFasade;

    @Before
    public void setUp() {
        sakConsumerMock = mock(SakConsumer.class);
        oppgaveConsumerMock = mock(OppgaveConsumer.class);
        gsakFasade = new GsakService(sakConsumerMock,oppgaveConsumerMock);
    }

    @Test
    public void testMappingMellomDTOogDomainForOppgave() throws MelosysException {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setId("1234");
        oppgaveDto.setSaksreferanse("456");
        oppgaveDto.setOppgavetype("BEH_SAK");
        oppgaveDto.setTema("MED");
        oppgaveDto.setSaksreferanse("MEL-111");

        when(oppgaveConsumerMock.hentOppgave("1234")).thenReturn(oppgaveDto);
        Oppgave oppgave = gsakFasade.hentOppgave("1234");
        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(oppgave.getSaksnummer()).isEqualTo("MEL-111");
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.valueOf("BEH_SAK"));
        assertThat(oppgave.getTema()).isEqualTo(Tema.valueOf("MED"));
    }
}
