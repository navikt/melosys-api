package no.nav.melosys.integrasjon.oppgave.konsument;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasadeImpl;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppgaveMappingMellomDTOogDomainTest {

    private OppgaveConsumer oppgaveConsumerMock;

    private OppgaveFasade oppgaveFasade;

    @Before
    public void setUp() {
        oppgaveConsumerMock = mock(OppgaveConsumer.class);
        oppgaveFasade = new OppgaveFasadeImpl(oppgaveConsumerMock);
    }

    @Test
    public void testMappingMellomDTOogDomainForOppgave() throws MelosysException {
        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setId("1234");
        oppgaveDto.setSaksreferanse("456");
        oppgaveDto.setOppgavetype("BEH_SAK_MK");
        oppgaveDto.setTema("MED");
        oppgaveDto.setSaksreferanse("MEL-111");

        when(oppgaveConsumerMock.hentOppgave("1234")).thenReturn(oppgaveDto);
        Oppgave oppgave = oppgaveFasade.hentOppgave("1234");
        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(oppgave.getSaksnummer()).isEqualTo("MEL-111");
        assertThat(oppgave.getOppgavetype()).isEqualTo(Oppgavetyper.valueOf("BEH_SAK_MK"));
        assertThat(oppgave.getTema()).isEqualTo(Tema.valueOf("MED"));
    }
}
