package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Lovvalgsperiode;

public interface LovvalgsperiodeRepository extends CrudRepository<Lovvalgsperiode, Long> {

    List<Lovvalgsperiode> findByBehandlingsresultatId(long id);
}
