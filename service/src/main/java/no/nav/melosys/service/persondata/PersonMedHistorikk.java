package no.nav.melosys.service.persondata;

import java.util.Collection;

import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

public record PersonMedHistorikk(
    Bostedsadresse bostedsadresse,
    Doedsfall dødsfall,
    Foedsel fødsel,
    Folkeregisteridentifikator folkeregisteridentifikator,
    KjoennType kjønn,
    Collection<Kontaktadresse> kontaktadresser,
    Navn navn,
    Collection<Oppholdsadresse> oppholdsadresser,
    Collection<Statsborgerskap> statsborgerskap
) {
}
