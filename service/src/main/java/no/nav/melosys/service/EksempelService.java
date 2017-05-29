package no.nav.melosys.service;

import no.nav.melosys.domain.Person;

import java.util.Date;
import java.util.List;

public interface EksempelService {

    List<Person> findAll();

    List<Person> findByDate(Date date);

    List<Person> findByEmail(String name);

    List<Person> findByNavn(String name);

    Person addPerson(Person person);

    Person updatePerson(Long id, Person updatedPerson);

    void deletePerson(Long id);
}
