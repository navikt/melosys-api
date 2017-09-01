
package no.nav.melosys.integrasjon.kodeverk;

import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

public interface KodeverkConsumer {

    public HentKodeverkResponse hentKodeverk(HentKodeverkRequest request) throws HentKodeverkHentKodeverkKodeverkIkkeFunnet;
    
}
