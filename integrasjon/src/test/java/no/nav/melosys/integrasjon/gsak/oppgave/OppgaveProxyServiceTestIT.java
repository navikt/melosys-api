package no.nav.melosys.integrasjon.gsak.oppgave;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import org.junit.Before;
import org.junit.Test;

import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class OppgaveProxyServiceTestIT extends Gen3WsProxyServiceITBase {
    private OppgaveSelftestConsumer selftestConsumer;
    private OppgaveConsumer oppgaveProxyService;

    @Autowired
    private OppgaveConsumerConfig consumerConfig;

    @Before
    public void setup() {
        OppgaveConsumerProducer oppgaveConsumerProducer = new OppgaveConsumerProducer();
        oppgaveConsumerProducer.setConfig(consumerConfig);

        OppgaveConsumerProducerDelegator producer = new OppgaveConsumerProducerDelegator(oppgaveConsumerProducer);
        selftestConsumer = producer.arbeidsfordelingSelftestConsumerForSystemUser();
        oppgaveProxyService = producer.arbeidsfordelingConsumerForEndUser();
    }

    @Test
    public void testPing() {
        selftestConsumer.ping();
    }

    // Testen vil være ok hvis vi oppretter oppgaver med oppgavetyper VUR_VL eller VUR_KONS_YTE_DAG først. Eller at disse finnes i GSAK.
    //@Test
    public void testFinnOppgaveListe() {
        // Arrange
        FinnOppgaveListeRequestMal requestMal = new FinnOppgaveListeRequestMal(lagFinnOppgaveListeSok(),
                lagFinnOppgaveListeFilter(), null, null);

        // Act
        FinnOppgaveListeResponse response = oppgaveProxyService.finnOppgaveListe(requestMal);

        // Assert
        assertThat(response).isNotNull();
    }

    private FinnOppgaveListeSokMal lagFinnOppgaveListeSok() {
        FinnOppgaveListeSokMal.Builder builder = FinnOppgaveListeSokMal.builder();
        return builder.medAnsvarligEnhetId("0219").build();
    }

    private FinnOppgaveListeFilterMal lagFinnOppgaveListeFilter() {
        List<String> oppgaveTyper = Arrays.asList("VUR_VL", "VUR_KONS_YTE_DAG");
        FinnOppgaveListeFilterMal.Builder builder = FinnOppgaveListeFilterMal.builder();
        return builder.medOppgavetypeKodeListe(oppgaveTyper).build();
    }
}
