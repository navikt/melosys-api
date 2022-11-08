package no.nav.melosys.domain.brev.trygdeavtale;

import java.util.Set;

import no.nav.melosys.domain.brev.Person;

public record MedfolgendeFamiliemedlemmer(Person ektefelle, Set<Person> barn) {
}
