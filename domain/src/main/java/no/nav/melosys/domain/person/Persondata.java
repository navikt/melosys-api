package no.nav.melosys.domain.person;

import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.domain.person.ident.Identifikator;

public record Persondata(Set<Identifikator> identer,
                         Navn navn,
                         Foedsel fødsel,
                         KjoennType kjoenn,
                         Collection<Adressebeskyttelse> adressebeskyttelser,
                         Collection<Statsborgerskap> statsborgerskap,
                         Collection<Sivilstand> sivilstand,
                         Folkeregisterpersonstatus folkeregisterpersonstatus,
                         Doedsfall doedsfall,
                         Set<Familiemedlem> familiemedlemmer) {

}
