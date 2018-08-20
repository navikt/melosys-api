package no.nav.melosys.integrasjon.inntk.inntekt;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;

public interface InntektConsumer {

    HentInntektListeBolkResponse hentInntektListeBolk(HentInntektListeBolkRequest request) throws HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter, HentInntektListeBolkUgyldigInput;
}
