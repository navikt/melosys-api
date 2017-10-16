package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;

import java.util.List;

public interface Medl2Fasade {

    @Deprecated
    List<Medlemsperiode> hentPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning;

    Saksopplysning getPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning;
}
