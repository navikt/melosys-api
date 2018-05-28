package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DokumentproduksjonConsumerTestIT {

    @Autowired
    DokumentproduksjonConsumerConfig config;

    private DokumentproduksjonSelftestConsumer selftestConsumer;

    @Before
    public void setUp() {
        DokumentproduksjonConsumerProducer producer = new DokumentproduksjonConsumerProducer();
        producer.setConfig(config);

        selftestConsumer = producer.dokumentproduksjonSelftestConsumer();
    }

    @Test
    public void ping() {
        selftestConsumer.ping();
    }
}
