package no.nav.melosys.service;

import no.nav.melosys.domain.PersonEks;

import java.util.Date;
import java.util.List;

public interface EksempelService {

    List<PersonEks> findAll();

    List<PersonEks> findByDate(Date date);

    List<PersonEks> findByEmail(String name);

    List<PersonEks> findByNavn(String name);

    PersonEks addPerson(PersonEks person);

    PersonEks updatePerson(Long id, PersonEks updatedPerson);

    void deletePerson(Long id);
}
