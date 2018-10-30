package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.VilkaarType;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.springframework.data.repository.CrudRepository;

public interface LovvalgsperiodeRepository extends CrudRepository<Lovvalgsperiode, Long> {

    Lovvalgsperiode findByBehandlingsresultatId(long ID);
}
