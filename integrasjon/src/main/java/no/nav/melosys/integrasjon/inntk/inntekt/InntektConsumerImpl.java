package no.nav.melosys.integrasjon.inntk.inntekt;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;

public class InntektConsumerImpl implements InntektConsumer {

    private InntektV3 port;

    public InntektConsumerImpl(InntektV3 port) {
        this.port = port;
    }

    @Override
    public HentInntektListeBolkResponse hentInntektListeBolk(HentInntektListeBolkRequest request) throws HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter, HentInntektListeBolkUgyldigInput {
        return port.hentInntektListeBolk(request);
    }
}
