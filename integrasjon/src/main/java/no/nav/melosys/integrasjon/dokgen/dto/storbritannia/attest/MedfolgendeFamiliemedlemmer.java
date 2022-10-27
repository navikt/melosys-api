package no.nav.melosys.integrasjon.dokgen.dto.storbritannia.attest;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.integrasjon.dokgen.dto.felles.Person;

public record MedfolgendeFamiliemedlemmer(Person ektefelle, List<Person> barn) {

    public static MedfolgendeFamiliemedlemmer av(no.nav.melosys.domain.brev.trygdeavtale.MedfolgendeFamiliemedlemmer familiemedlemmer) {
        if (familiemedlemmer == null) return null;

        return new MedfolgendeFamiliemedlemmer(
            Person.av(familiemedlemmer.ektefelle()),
            familiemedlemmer.barn().stream().map(Person::av).collect(Collectors.toList())
        );
    }
}
