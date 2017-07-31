package no.nav.melosys.repository;


import java.util.List;

import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Bruker;

public interface BrukerRepository extends CrudRepository<Bruker, Long> {

    List<Bruker> findByNavn(String navn);

    Bruker findByFnr(String fnr);
}