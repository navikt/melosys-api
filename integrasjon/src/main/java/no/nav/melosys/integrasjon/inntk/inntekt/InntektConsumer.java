package no.nav.melosys.integrasjon.inntk.inntekt;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;

public interface InntektConsumer {

    HentInntektListeResponse hentInntektListe(HentInntektListeRequest request) throws HentInntektListeSikkerhetsbegrensning, HentInntektListeUgyldigInput, HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter;

    HentInntektListeBolkResponse hentInntektListeBolk(HentInntektListeBolkRequest request) throws HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter, HentInntektListeBolkUgyldigInput;
}
