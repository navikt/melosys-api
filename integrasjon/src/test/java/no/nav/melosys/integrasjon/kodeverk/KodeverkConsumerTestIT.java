package no.nav.melosys.integrasjon.kodeverk;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.kodeverk.impl.KodeverkConsumer;
import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

@RunWith(SpringRunner.class)
public class KodeverkConsumerTestIT extends Gen3WsProxyServiceITBase {

    private static final String LANDKODER = "Landkoder";

    @Autowired
    private KodeverkConsumer kodeverkConsumer;
    
    @Test
    public void pingTest() {
        kodeverkConsumer.ping();
    }

    @Test
    public void hentLandkoderTest() throws Exception {
        HentKodeverkRequest req = new HentKodeverkRequest();
        req.setNavn(LANDKODER);
        
        HentKodeverkResponse res = kodeverkConsumer.hentKodeverk(req);
        assertNotNull(res);
    }

}