package no.nav.melosys.integrasjon.pdl.dto.person;

import java.util.Collection;

import no.nav.melosys.integrasjon.pdl.dto.person.adresse.kontakt.Kontaktadresse;
import no.nav.melosys.integrasjon.pdl.dto.person.adresse.Kontaktadresse;

public record Person(
    Collection<Adressebeskyttelse> adressebeskyttelse,
    Collection<Doedsfall> doedsfall,
    Collection<Foedsel> foedsel,
    Collection<Folkeregisteridentifikator> folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatus,
    Collection<ForelderBarnRelasjon> forelderBarnRelasjon,
    Collection<Kjoenn> kjoenn,
    Collection<Kontaktadresse> kontaktadresse,
    Collection<Navn> navn,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap
) {
}
