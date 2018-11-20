package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface VilkaarsresultatRepository extends CrudRepository<Vilkaarsresultat, Long> {

    List<Vilkaarsresultat> findByBehandlingsresultatId(long ID);

    void deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);
}
