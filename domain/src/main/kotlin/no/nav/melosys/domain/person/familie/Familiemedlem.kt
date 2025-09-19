package no.nav.melosys.domain.person.familie;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Sivilstand;

public record Familiemedlem(
    Folkeregisteridentifikator folkeregisteridentifikator,
    Navn navn,
    Familierelasjon familierelasjon,
    Foedsel fødsel,
    Folkeregisteridentifikator folkeregisteridentAnnenForelder,
    String foreldreansvarstype,
    Sivilstand sivilstand) {
    public boolean erBarn() {
        return familierelasjon == Familierelasjon.BARN;
    }

    public boolean erForelder() {
        return familierelasjon == Familierelasjon.FAR || familierelasjon == Familierelasjon.MOR;
    }

    public boolean erRelatertVedSivilstand() {
        return familierelasjon == Familierelasjon.RELATERT_VED_SIVILSTAND;
    }
}
