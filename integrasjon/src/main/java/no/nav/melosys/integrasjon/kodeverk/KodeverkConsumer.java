
package no.nav.melosys.integrasjon.kodeverk;

import no.nav.melosys.integrasjon.felles.SelftestConsumer;
import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

public interface KodeverkConsumer extends SelftestConsumer {

    public HentKodeverkResponse hentKodeverk(HentKodeverkRequest request) throws HentKodeverkHentKodeverkKodeverkIkkeFunnet;
    
}
