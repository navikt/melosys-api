package no.nav.melosys.repository;


import no.nav.melosys.domain.PersonEks;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface PersonRepository extends CrudRepository<PersonEks, Long> {

    List<PersonEks> findByDate(Date date);

    List<PersonEks> findByEmail(String email);

    List<PersonEks> findByNavn(String navn);

}