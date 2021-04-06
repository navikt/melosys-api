package no.nav.melosys.integrasjon.pdl.dto.person;

import java.util.Collection;

public record Person(
    Collection<Adressebeskyttelse> adressebeskyttelse,
    Collection<Doedsfall> doedsfall,
    Collection<Foedsel> foedsel,
    Collection<Folkeregisteridentifikator> folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatus,
    Collection<ForelderBarnRelasjon> forelderBarnRelasjon,
    Collection<Kjoenn> kjoenn,
    Collection<Navn> navn,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap
) {
}
