package no.nav.melosys.integrasjon.joark.journal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;

@RunWith(SpringRunner.class)
public class JournalSelftestConsumerTestIT extends Gen3WsProxyServiceITBase {
    private JournalSelftestConsumer journalSelftestConsumer;

    @Autowired
    private JournalConsumerConfig consumerConfig;

    @Before
    public void setup() throws Exception {
        JournalConsumerProducer consumerProducer = new JournalConsumerProducer();
        consumerProducer.setConfig(consumerConfig);
        journalSelftestConsumer = consumerProducer.journalSelftestConsumer();
    }

    @Test
    public void test_ping() {
        journalSelftestConsumer.ping();
    }
}