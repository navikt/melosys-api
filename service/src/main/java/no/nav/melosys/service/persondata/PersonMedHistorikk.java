package no.nav.melosys.service.persondata;

import java.util.Collection;

import no.nav.melosys.domain.person.*;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

public record PersonMedHistorikk(
    Collection<Bostedsadresse> bostedsadresser,
    Doedsfall dødsfall,
    Foedsel fødsel,
    Folkeregisteridentifikator folkeregisteridentifikator,
    Folkeregisterpersonstatus folkeregisterpersonstatus,
    KjoennType kjønn,
    Collection<Kontaktadresse> kontaktadresser,
    Navn navn,
    Collection<Oppholdsadresse> oppholdsadresser,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap) {
}
