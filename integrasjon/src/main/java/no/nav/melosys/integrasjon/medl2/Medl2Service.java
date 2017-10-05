package no.nav.melosys.integrasjon.medl2;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Periode;

import java.util.List;

public class Medl2Service implements Medl2Fasade {

    @Override
    public List<Periode> hentPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        return null;
    }
}
