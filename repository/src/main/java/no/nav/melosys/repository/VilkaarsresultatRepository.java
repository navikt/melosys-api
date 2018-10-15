package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.VilkaarType;
import no.nav.melosys.domain.Vilkaarsresultat;
import org.springframework.data.repository.CrudRepository;

public interface VilkaarsresultatRepository extends CrudRepository<Vilkaarsresultat, Long> {

    List<Vilkaarsresultat> findByBehandlingsresultatId(long ID);

    Vilkaarsresultat findByBehandlingsresultatIdAndVilkaar(long ID, VilkaarType vilkaarType);
}
