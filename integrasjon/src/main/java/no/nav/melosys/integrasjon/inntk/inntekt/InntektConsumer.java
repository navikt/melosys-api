package no.nav.melosys.integrasjon.inntk.inntekt;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeUgyldigInput;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;

public interface InntektConsumer {
    HentInntektListeResponse hentInntektListe(HentInntektListeRequest request) throws HentInntektListeSikkerhetsbegrensning, HentInntektListeUgyldigInput, HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter;
}
