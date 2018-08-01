package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class BehandlingskjedeConsumerTestIT {

    @Autowired
    BehandlingskjedeConsumerConfig config;

    private BehandlingskjedeSelftestConsumer selftestConsumer;

    @Before
    public void setUp() {
        BehandlingskjedeConsumerProducer producer = new BehandlingskjedeConsumerProducer();
        producer.setConfig(config);

        selftestConsumer = producer.behandlingskjedeSelftestConsumer();
    }

    @Test
    public void test_ping() {
        selftestConsumer.ping();
    }
}
