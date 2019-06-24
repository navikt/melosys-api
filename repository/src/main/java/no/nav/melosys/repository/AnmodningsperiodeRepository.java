package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.repository.CrudRepository;

public interface AnmodningsperiodeRepository extends CrudRepository<Anmodningsperiode, Long> {
    List<Anmodningsperiode> findByBehandlingsresultatId(long id);

    List<Anmodningsperiode> deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);

}
