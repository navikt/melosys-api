package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;

public interface BehandleMedlemskapConsumer {

    OpprettPeriodeResponse opprettPeriode(OpprettPeriodeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning, UgyldigInput;
}
