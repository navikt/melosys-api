package no.nav.melosys.repository;


import org.springframework.data.repository.CrudRepository;

// FIXME
public interface BrukerRepository extends CrudRepository<Object, Long> {

    Object findByFnr(String fnr);
}