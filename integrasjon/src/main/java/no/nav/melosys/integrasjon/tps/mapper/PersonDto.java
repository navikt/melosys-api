package no.nav.melosys.integrasjon.tps.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.dokument.person.Person;

public class PersonDto {
    public Person person;
    public Map<String, Person> familiemedlemmer = new HashMap<>();
    public List<String> kilder = new ArrayList<>();

    public PersonDto(Person person, String dokumentXml) {
        this.person = person;
        this.kilder.add(dokumentXml);
    }

    public void leggTilFamiliemedlem(PersonDto familiemedlem) {
        this.familiemedlemmer.put(familiemedlem.person.fnr, familiemedlem.person);
        this.kilder.add(familiemedlem.kilder.get(0));
    }
}
