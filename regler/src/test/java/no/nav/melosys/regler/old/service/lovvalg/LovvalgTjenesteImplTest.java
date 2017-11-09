package no.nav.melosys.regler.service.lovvalg;

import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Test;

import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRequest;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRespons;

public class LovvalgTjenesteImplTest {

    @Test
    public void testService() {
        LovvalgTjenesteImpl service = new LovvalgTjenesteImpl();
        FastsettLovvalgRequest søknad = lagFastsettLovvalgRequest();
        
        FastsettLovvalgRespons respons = service.fastsettLovvalg(søknad);
        assertNotNull(respons);
    }

    public static FastsettLovvalgRequest lagFastsettLovvalgRequest() {
        FastsettLovvalgRequest søknad = new FastsettLovvalgRequest();
        søknad.arbeidstakerEllerSelvstendigNaeringsdrivende = true;
        søknad.periodeFom = LocalDate.of(2017, 1, 1);
        søknad.periodeTom = LocalDate.of(2018, 12, 31);
        søknad.land = Collections.singletonList("Agurkland");
        return søknad;
    }
}
