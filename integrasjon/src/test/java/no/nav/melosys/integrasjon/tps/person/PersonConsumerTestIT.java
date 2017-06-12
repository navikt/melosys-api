package no.nav.melosys.integrasjon.tps.person;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.person.v2.binding.HentKjerneinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v2.binding.HentKjerneinformasjonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v2.meldinger.HentKjerneinformasjonRequest;
import no.nav.tjeneste.virksomhet.person.v2.meldinger.HentKjerneinformasjonResponse;

@RunWith(SpringRunner.class)
public class PersonConsumerTestIT extends Gen3WsProxyServiceITBase {

    private PersonConsumer personConsumer;

    @Autowired
    private PersonConsumerConfig consumerConfig;

    @Before
    public void setup() throws Exception {
        PersonConsumerProducer consumerProducer = new PersonConsumerProducer();
        consumerProducer.setConfig(consumerConfig);
        personConsumer = consumerProducer.personConsumer();
    }

    @Test
    public void test_hentKjerneinformasjon() throws HentKjerneinformasjonPersonIkkeFunnet, HentKjerneinformasjonSikkerhetsbegrensning {
        HentKjerneinformasjonRequest request = new HentKjerneinformasjonRequest();
        request.setIdent(TpsTestData.STD_KVINNE_FNR);
        HentKjerneinformasjonResponse response = personConsumer.hentKjerneinformasjon(request);
        assertNotNull(response.getPerson());
    }

}
