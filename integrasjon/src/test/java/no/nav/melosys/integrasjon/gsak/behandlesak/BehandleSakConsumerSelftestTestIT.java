package no.nav.melosys.integrasjon.gsak.behandlesak;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;

@Ignore
@RunWith(SpringRunner.class)
public class BehandleSakConsumerSelftestTestIT extends Gen3WsProxyServiceITBase {

    private BehandleSakSelftestConsumer behandleSakSelftestConsumer;

    @Autowired
    private BehandleSakConsumerConfig behandleSakConfig;

    @Before
    public void setup() throws Exception {
        BehandleSakConsumerProducer behandleSakConsumerProducer = new BehandleSakConsumerProducer();
        behandleSakConsumerProducer.setConfig(behandleSakConfig);
        behandleSakSelftestConsumer = behandleSakConsumerProducer.behandleSakSelftestConsumer();
    }

    @Test
    public void test_ping() {
        behandleSakSelftestConsumer.ping();
    }
}
