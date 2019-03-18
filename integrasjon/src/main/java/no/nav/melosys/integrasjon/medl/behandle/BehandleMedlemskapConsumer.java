package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.*;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OppdaterPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeRequest;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.meldinger.OpprettPeriodeResponse;

public interface BehandleMedlemskapConsumer {

    OpprettPeriodeResponse opprettPeriode(OpprettPeriodeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning, UgyldigInput;

    void oppdaterPeriode(OppdaterPeriodeRequest request) throws Sikkerhetsbegrensning, UgyldigInput, PeriodeUtdatert, PeriodeIkkeFunnet;
}
