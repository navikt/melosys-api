package no.nav.melosys.integrasjon.tps.person;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.End2EndTest;
import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;

@RunWith(SpringRunner.class)
@Category(End2EndTest.class)
public class PersonSelftestConsumerTestIT extends Gen3WsProxyServiceITBase {

    private PersonSelftestConsumer personSelftestConsumer;

    @Autowired
    private PersonConsumerConfig consumerConfig;

    @Before
    public void setup() throws Exception {

        PersonConsumerProducer consumerProducer = new PersonConsumerProducer();
        consumerProducer.setConfig(consumerConfig);
        personSelftestConsumer = consumerProducer.personSelftestConsumer();
    }

    @Test
    public void test_ping() {
        personSelftestConsumer.ping();
    }

}
