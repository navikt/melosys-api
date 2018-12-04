package no.nav.melosys.integrasjon.kodeverk.impl;

import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties")
public class KodeverkRegisterTestIT {

    private static final String LANDKODER = "Landkoder";

    @Value("${KodeverkAPI_v1.url}")
    String endpointUrl;

    private KodeverkRegisterImpl kodeverkRegister;

    @Before
    public void setUp() {
        KodeverkConsumerProducer producer = new KodeverkConsumerProducer(endpointUrl);
        kodeverkRegister = new KodeverkRegisterImpl(producer.kodeverkConsumer());
    }

    @Test
    public void hentLandkoderTest() throws Exception {
        Kodeverk kodeverk = kodeverkRegister.hentKodeverk(LANDKODER);
        assertThat(kodeverk).isNotNull();
    }

}
