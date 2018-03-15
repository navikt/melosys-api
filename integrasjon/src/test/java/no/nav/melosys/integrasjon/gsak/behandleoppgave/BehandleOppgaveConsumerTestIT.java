package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class BehandleOppgaveConsumerTestIT {

    @Autowired
    BehandleOppgaveConsumerConfig config;

    private BehandleOppgaveConsumer consumer;

    private BehandleOppgaveSelftestConsumer selftestConsumer;

    @Before
    public void setUp() {
        BehandleOppgaveConsumerProducer producer = new BehandleOppgaveConsumerProducer();
        producer.setConfig(config);

        consumer = producer.behandleOppgaveConsumer();
        selftestConsumer = producer.behandleOppgaveSelftestConsumer();
    }

    @Test
    public void test_ping() {
        selftestConsumer.ping();
    }
}