package no.nav.melosys.integrasjon.gsak.behandlesak;

import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakResponse;

public interface BehandleSakConsumer {
    OpprettSakResponse opprettSak(OpprettSakRequest request)
            throws OpprettSakSakEksistererAllerede, OpprettSakUgyldigInput;
}
