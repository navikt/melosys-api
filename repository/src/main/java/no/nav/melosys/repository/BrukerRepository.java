package no.nav.melosys.repository;


import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Bruker;

public interface BrukerRepository extends CrudRepository<Bruker, Long> {

    Bruker findByFnr(String fnr);
}