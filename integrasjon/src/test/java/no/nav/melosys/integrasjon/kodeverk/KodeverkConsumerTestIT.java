package no.nav.melosys.integrasjon.kodeverk;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

@RunWith(SpringRunner.class)
public class KodeverkConsumerTestIT extends Gen3WsProxyServiceITBase {

    private static final String LANDKODER = "Landkoder";

    @Autowired
    private KodeverkConsumerConfig config;

    private KodeverkConsumer kodeverkConsumer;
    private KodeverkSelftestConsumer kodeverkSelftestConsumer;

    @Before
    public void setup() throws Exception {
        KodeverkConsumerProducer producer = new KodeverkConsumerProducer();
        ReflectionTestUtils.setField(producer, "consumerConfig", config);

        kodeverkConsumer = producer.kodeverkConsumer();
        kodeverkSelftestConsumer = producer.kodeverkSelftestConsumer();
    }

    @Test
    public void pingTest() {
        kodeverkSelftestConsumer.ping();
    }

    @Test
    public void hentLandkoderTest() throws Exception {
        HentKodeverkRequest req = new HentKodeverkRequest();
        req.setNavn(LANDKODER);
        
        HentKodeverkResponse res = kodeverkConsumer.hentKodeverk(req);
        assertNotNull(res);
    }

}