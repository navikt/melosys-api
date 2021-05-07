package no.nav.melosys.integrasjon.pdl.dto.person;

import java.util.Collection;

import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Kontaktadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Oppholdsadresse;

public record Person(
    Collection<Adressebeskyttelse> adressebeskyttelse,
    Collection<Bostedsadresse> bostedsadresse,
    Collection<Doedsfall> doedsfall,
    Collection<Foedsel> foedsel,
    Collection<Folkeregisteridentifikator> folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatus,
    Collection<ForelderBarnRelasjon> forelderBarnRelasjon,
    Collection<Fullmakt> fullmakt,
    Collection<Kjoenn> kjoenn,
    Collection<Kontaktadresse> kontaktadresse,
    Collection<Navn> navn,
    Collection<Oppholdsadresse> oppholdsadresse,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap,
    Collection<UtenlandskIdentifikasjonsnummer> utenlandskIdentifikasjonsnummer
) {
}
