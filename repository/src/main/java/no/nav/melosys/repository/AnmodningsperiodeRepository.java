package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandling.Behandlingsresultat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnmodningsperiodeRepository extends JpaRepository<Anmodningsperiode, Long> {
    List<Anmodningsperiode> findByBehandlingsresultatId(long id);

    List<Anmodningsperiode> deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);

}
