package no.nav.melosys.integrasjon.medl2;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Periode;

import java.util.List;

public interface Medl2Fasade {

    List<Periode> hentPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning;
}
