package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class BehandleMedlemskapConsumerTestIT extends Gen3WsProxyServiceITBase {

    @Autowired
    BehandleMedlemskapConsumerConfig config;

    private BehandleMedlemskapSelftestConsumer selftestConsumer;

    @Before
    public void setUp() {
        BehandleMedlemskapConsumerProducer producer = new BehandleMedlemskapConsumerProducer();
        producer.setConfig(config);

        selftestConsumer = producer.getSelftestConsumer();
    }

    @Test
    public void ping() {
        selftestConsumer.ping();
    }
}
