package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sak.SakConsumer;
import no.nav.melosys.integrasjon.sak.dto.SakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArkivsakServiceTest {
    @Mock
    private SakConsumer sakConsumer;

    private ArkivsakService arkivsakService;

    @Captor
    private ArgumentCaptor<SakDto> captor;

    @Before
    public void setup() {
        arkivsakService = new ArkivsakService(sakConsumer);
    }

    @Test
    public void opprettSak_behandlingstypeSøknad_temaMed() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-123";
        final Behandlingstema behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSak(saksnummer, behandlingstema, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.MED.getKode());
    }

    @Test
    public void opprettSak_behandlingstypeRegistreringUnntak_temaUfm() throws FunksjonellException, TekniskException {
        final String saksnummer = "MEL-123";
        final Behandlingstema behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSak(saksnummer, behandlingstema, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    public void hentTemaFraSak_temaErUfm_forventUfm() throws FunksjonellException, TekniskException {
        final Long sakID = 11111L;
        SakDto sakDto = new SakDto();
        sakDto.setTema(Tema.UFM.getKode());
        when(sakConsumer.hentSak(eq(sakID))).thenReturn(sakDto);

        Tema tema = arkivsakService.hentTemaFraSak(sakID);
        assertThat(tema).isEqualTo(Tema.UFM);
    }
}