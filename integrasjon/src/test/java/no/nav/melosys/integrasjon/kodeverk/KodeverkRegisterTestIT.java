package no.nav.melosys.integrasjon.kodeverk;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;

@RunWith(SpringRunner.class)
public class KodeverkRegisterTestIT extends Gen3WsProxyServiceITBase {

    private static final String LANDKODER = "Landkoder";

    @Autowired
    private KodeverkRegister kodeverkregister;

    @Test
    public void hentLandkoderTest() throws Exception {
        Kodeverk res = kodeverkregister.hentKodeverk(LANDKODER);
        assertNotNull(res);
    }

}