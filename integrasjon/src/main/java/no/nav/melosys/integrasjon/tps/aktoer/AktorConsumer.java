package no.nav.melosys.integrasjon.tps.aktoer;

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentListeRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentListeResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdListeRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdListeResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;

public interface AktorConsumer {
    HentAktoerIdForIdentResponse hentAktørIdForIdent(HentAktoerIdForIdentRequest request)
            throws HentAktoerIdForIdentPersonIkkeFunnet;

    HentAktoerIdForIdentListeResponse hentAktoerIdForIdentListeRequest(HentAktoerIdForIdentListeRequest request);

    HentIdentForAktoerIdResponse hentIdentForAktoerId(HentIdentForAktoerIdRequest request)
            throws HentIdentForAktoerIdPersonIkkeFunnet;

    HentIdentForAktoerIdListeResponse hentIdentForAktoerIdListeRequest(HentIdentForAktoerIdListeRequest request);
}
