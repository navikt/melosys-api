package no.nav.melosys.integrasjon.kodeverk;

import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.KodeverkPortType;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

public class KodeverkConsumerImpl implements KodeverkConsumer {

    private KodeverkPortType port;

    public KodeverkConsumerImpl(KodeverkPortType port) {
        this.port = port;
    }

    @Override
    public HentKodeverkResponse hentKodeverk(HentKodeverkRequest request) throws HentKodeverkHentKodeverkKodeverkIkkeFunnet {
        return port.hentKodeverk(request);
    }

}
