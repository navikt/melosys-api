package no.nav.melosys.service.sak;

import no.finn.unleash.FakeUnleash;
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

    private FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    public void setup() {
        arkivsakService = new ArkivsakService(sakConsumer, unleash);
    }

    @Test
    public void opprettSak_behandlingstypeSøknad_temaMed() {
        final String saksnummer = "MEL-123";
        final Behandlingstema behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER;
        final Sakstemaer sakstemaer = Sakstemaer.MEDLEMSKAP_LOVVALG;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, behandlingstema, sakstemaer, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.MED.getKode());
    }

    @Test
    public void opprettSak_behandlingstypeRegistreringUnntak_temaUfm() {
        final String saksnummer = "MEL-123";
        final Behandlingstema behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        final Sakstemaer sakstemaer = Sakstemaer.MEDLEMSKAP_LOVVALG;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, behandlingstema, sakstemaer, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.UFM.getKode());
    }

    @Test
    public void hentTemaFraSak_temaErUfm_forventUfm() {
        final Long sakID = 11111L;
        SakDto sakDto = new SakDto();
        sakDto.setTema(Tema.UFM.getKode());
        when(sakConsumer.hentSak(sakID)).thenReturn(sakDto);

        Tema tema = arkivsakService.hentTemaFraSak(sakID);
        assertThat(tema).isEqualTo(Tema.UFM);
    }

    @Test
    void opprettSak_sakstemaerMEDLEMSKAP_forventMED() {
        unleash.enable("melosys.behandle_alle_saker");
        final String saksnummer = "MEL-123";
        final Behandlingstema behandlingstema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        final Sakstemaer sakstemaer = Sakstemaer.MEDLEMSKAP_LOVVALG;
        final String aktørID = "123123123";
        final Long sakID = 1111L;

        SakDto sakDto = new SakDto();
        sakDto.setId(sakID);
        when(sakConsumer.opprettSak(any())).thenReturn(sakDto);

        Long opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, behandlingstema, sakstemaer, aktørID);

        assertThat(opprettetSakID).isEqualTo(sakID);
        verify(sakConsumer).opprettSak(captor.capture());

        SakDto opprettetSakDto = captor.getValue();
        assertThat(opprettetSakDto.getTema()).isEqualTo(Tema.MED.getKode());
    }
}
