package no.nav.melosys.domain.person.familie;

import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.Navn;

public record Familiemedlem(
    Folkeregisteridentifikator folkeregisteridentifikator,
    Navn navn,
    Familierelasjon familierelasjon,
    Foedsel fødsel
) {
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
