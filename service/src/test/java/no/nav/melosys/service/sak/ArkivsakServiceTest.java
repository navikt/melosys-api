package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArkivsakServiceTest {
    @Mock
    private SakConsumer sakConsumer;

    private ArkivsakService arkivsakService;

    @Captor
    private ArgumentCaptor<SakDto> captor;

    @BeforeEach
    public void setup() {
        arkivsakService = new ArkivsakService(sakConsumer);
    }

    @Test
    public void opprettSakForBruker_behandlingstypeFørstegang_temaMed() {
        final String saksnummer = "MEL-123";
        final Tema tema = Tema.MED;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.MED.getKode());
    }

    @Test
    public void opprettSakForBruker_behandlingstypeRegistreringUnntak_temaUfm() {
        final String saksnummer = "MEL-123";
        final Tema tema = Tema.UFM;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    public void opprettSakForVirksomhet_behandlingstypeFørstegang_temaMed() {
        final String saksnummer = "MEL-123";
        final Tema tema = Tema.MED;
        final String orgId = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, orgId);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.MED.getKode());
    }

    @Test
    public void opprettSakForVirksomhet_behandlingstypeRegistreringUnntak_temaUfm() {
        final String saksnummer = "MEL-123";
        final Tema tema = Tema.UFM;
        final String orgId = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, orgId);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.UFM.getKode());
    }
}
