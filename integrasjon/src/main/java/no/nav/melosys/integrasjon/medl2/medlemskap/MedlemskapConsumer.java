package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;

public interface MedlemskapConsumer {

    HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest var1) throws PersonIkkeFunnet, Sikkerhetsbegrensning;
}
