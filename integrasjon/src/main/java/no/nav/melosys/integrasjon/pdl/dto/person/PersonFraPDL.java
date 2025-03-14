package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Kontaktadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Oppholdsadresse;

import java.util.Collection;

public record PersonFraPDL(
    Collection<Adressebeskyttelse> adressebeskyttelse,
    Collection<Bostedsadresse> bostedsadresse,
    Collection<Doedsfall> doedsfall,
    Collection<Foedested> foedested,
    Collection<Foedselsdato> foedselsdato,
    Collection<Folkeregisteridentifikator> folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatus,
    Collection<ForelderBarnRelasjon> forelderBarnRelasjon,
    Collection<Foreldreansvar> foreldreansvar,
    Collection<Kjoenn> kjoenn,
    Collection<Kontaktadresse> kontaktadresse,
    Collection<Navn> navn,
    Collection<Oppholdsadresse> oppholdsadresse,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap,
    Collection<UtenlandskIdentifikasjonsnummer> utenlandskIdentifikasjonsnummer
) {
}
