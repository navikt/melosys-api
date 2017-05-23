package no.nav.melosys.repository;


import no.nav.melosys.domain.Person;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public interface PersonRepository extends CrudRepository<Person, Long> {

    List<Person> findByDate(Date date);

    List<Person> findByEmail(String email);

    List<Person> findByNavn(String navn);

}